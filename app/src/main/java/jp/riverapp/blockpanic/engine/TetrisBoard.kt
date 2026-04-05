package jp.riverapp.blockpanic.engine

// 1:1 port from shared/game/TetrisBoard.ts

import kotlin.math.floor
import kotlin.random.Random

data class ActivePiece(
    var type: Int,            // 0-6
    var rotation: Int,        // 0-3 current
    var col: Int,             // current grid column
    var pixelY: Double,       // current y in pixels (for smooth falling)
    var targetRotation: Int,
    var targetCol: Int,
    var speedMultiplier: Double // 1.0~3.0 per-piece random speed
)

class TetrisBoard(
    private val boardIndex: Int,
    initialDelay: Double
) {
    var grid: Array<IntArray> = Array(C.boardRows) { IntArray(C.boardCols) }  // BOARD_ROWS x BOARD_COLS, 0 = empty
    var currentPiece: ActivePiece? = null
    private var spawnTimer: Double = initialDelay
    private var aiMoveTicker: Int = 0
    private var aiMoveInterval: Int = 3       // ticks between AI moves

    private var baseFallSpeed: Double = 2.0   // pixels per tick
    private var spawnDelay: Double = 400.0    // ms after landing before next spawn

    fun update(dtMs: Double) {
        if (currentPiece == null) {
            spawnTimer -= dtMs
            if (spawnTimer <= 0) {
                spawnPiece()
            }
            return
        }

        // AI movement
        aiMoveTicker += 1
        if (aiMoveTicker >= aiMoveInterval) {
            aiMoveTicker = 0
            executeAIMove()
        }

        // Fall
        currentPiece!!.pixelY += getFallSpeed()

        // Landing check
        val gridRow = floor(currentPiece!!.pixelY / C.cellSize).toInt()
        val cells = pieceRotations[currentPiece!!.type][currentPiece!!.rotation]

        if (wouldCollide(cells, currentPiece!!.col, gridRow + 1)) {
            currentPiece!!.pixelY = gridRow.toDouble() * C.cellSize
            landPiece(gridRow)
        }
    }

    private fun getFallSpeed(): Double {
        return baseFallSpeed * (currentPiece?.speedMultiplier ?: 1.0)
    }

    private fun spawnPiece() {
        val type = Random.nextInt(7)
        val cells = pieceRotations[type][0]
        val spawnCol = (C.boardCols - pieceWidth(cells)) / 2

        // AI decides placement
        val best = findBestPlacement(grid, type)

        // Random speed: 1.0x (normal) ~ 4.0x (fast)
        val speedMultiplier = 1.0 + Random.nextDouble() * 3.0

        currentPiece = ActivePiece(
            type = type,
            rotation = 0,
            col = spawnCol,
            pixelY = -C.cellSize * 2,
            targetRotation = best.rotation,
            targetCol = best.col,
            speedMultiplier = speedMultiplier
        )
    }

    private fun executeAIMove() {
        val piece = currentPiece ?: return

        // Rotate toward target first
        if (piece.rotation != piece.targetRotation) {
            val nextRotation = (piece.rotation + 1) % 4
            val nextCells = pieceRotations[piece.type][nextRotation]
            val gridRow = floor(piece.pixelY / C.cellSize).toInt()
            if (!wouldCollide(nextCells, piece.col, gridRow)) {
                piece.rotation = nextRotation
            }
            return // One action per tick
        }

        // Move toward target column
        if (piece.col < piece.targetCol) {
            val cells = pieceRotations[piece.type][piece.rotation]
            val gridRow = floor(piece.pixelY / C.cellSize).toInt()
            if (!wouldCollide(cells, piece.col + 1, gridRow)) {
                piece.col += 1
            }
        } else if (piece.col > piece.targetCol) {
            val cells = pieceRotations[piece.type][piece.rotation]
            val gridRow = floor(piece.pixelY / C.cellSize).toInt()
            if (!wouldCollide(cells, piece.col - 1, gridRow)) {
                piece.col -= 1
            }
        }
    }

    private fun wouldCollide(cells: Array<IntArray>, col: Int, row: Int): Boolean {
        for (cell in cells) {
            val dr = cell[0]
            val dc = cell[1]
            val r = row + dr
            val c = col + dc
            if (c < 0 || c >= C.boardCols) return true
            if (r >= C.boardRows) return true
            if (r >= 0 && grid[r][c] != 0) return true
        }
        return false
    }

    private fun landPiece(row: Int) {
        val piece = currentPiece ?: return

        val cells = pieceRotations[piece.type][piece.rotation]
        val value = piece.type + 1 // 1-7 (0 = empty)

        for (cell in cells) {
            val dr = cell[0]
            val dc = cell[1]
            val r = row + dr
            val c = piece.col + dc
            if (r in 0 until C.boardRows && c in 0 until C.boardCols) {
                grid[r][c] = value
            }
        }

        clearLines()

        // Check if blocks reached the top -- if so, reset this board
        if (isTopOut()) {
            reset()
            return
        }

        currentPiece = null
        spawnTimer = spawnDelay
    }

    /** Check if any block exists in the top 2 rows (game over condition) */
    private fun isTopOut(): Boolean {
        for (row in 0 until 2) {
            for (col in 0 until C.boardCols) {
                if (grid[row][col] != 0) return true
            }
        }
        return false
    }

    private fun clearLines() {
        var writeRow = C.boardRows - 1

        for (readRow in (C.boardRows - 1) downTo 0) {
            if (grid[readRow].all { it != 0 }) {
                // Full line -- skip (clear)
                continue
            }
            if (writeRow != readRow) {
                for (c in 0 until C.boardCols) {
                    grid[writeRow][c] = grid[readRow][c]
                }
            }
            writeRow -= 1
        }

        // Fill top with empty
        for (row in writeRow downTo 0) {
            for (c in 0 until C.boardCols) {
                grid[row][c] = 0
            }
        }
    }

    /** Convert grid + falling piece to BlockState[] for network broadcast */
    fun getBlocks(xOffset: Double): List<BlockState> {
        val blocks = mutableListOf<BlockState>()

        // Landed blocks from grid
        for (row in 0 until C.boardRows) {
            for (col in 0 until C.boardCols) {
                val v = grid[row][col]
                if (v != 0) {
                    blocks.add(BlockState(
                        id = "b-$boardIndex-$row-$col",
                        x = xOffset + col.toDouble() * C.cellSize,
                        y = row.toDouble() * C.cellSize,
                        width = C.cellSize,
                        height = C.cellSize,
                        falling = false,
                        pieceType = v - 1
                    ))
                }
            }
        }

        // Falling piece
        val piece = currentPiece
        if (piece != null) {
            val cells = pieceRotations[piece.type][piece.rotation]
            for (cell in cells) {
                val dr = cell[0]
                val dc = cell[1]
                val px = xOffset + (piece.col + dc).toDouble() * C.cellSize
                val py = piece.pixelY + dr.toDouble() * C.cellSize
                blocks.add(BlockState(
                    id = "f-$boardIndex-$dr-$dc",
                    x = px,
                    y = py,
                    width = C.cellSize,
                    height = C.cellSize,
                    falling = true,
                    pieceType = piece.type
                ))
            }
        }

        return blocks
    }

    fun reset() {
        grid = Array(C.boardRows) { IntArray(C.boardCols) }
        currentPiece = null
        spawnTimer = 0.0
    }
}
