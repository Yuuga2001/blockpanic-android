package jp.riverapp.blockpanic.engine

// 1:1 port from shared/game/TetrisAI.ts

import kotlin.math.abs

data class Placement(
    val rotation: Int,
    val col: Int
)

/** Find the best placement for a piece on the given board grid */
fun findBestPlacement(grid: Array<IntArray>, pieceType: Int): Placement {
    var bestScore = Double.NEGATIVE_INFINITY
    var best = Placement(rotation = 0, col = 0)

    for (rotation in 0 until 4) {
        val cells = pieceRotations[pieceType][rotation]
        val w = pieceWidth(cells)
        val maxCol = C.boardCols - w

        for (col in 0..maxCol) {
            val dropRow = findDropRow(grid, cells, col)
            if (dropRow < 0) continue // Can't place

            // Simulate placement
            val simGrid = copyGrid(grid)
            placePiece(simGrid, cells, col, dropRow, pieceType + 1)
            val linesCleared = countAndClearLines(simGrid)

            val score = evaluate(simGrid, linesCleared)
            if (score > bestScore) {
                bestScore = score
                best = Placement(rotation = rotation, col = col)
            }
        }
    }

    return best
}

/** Find the lowest row where the piece can land */
private fun findDropRow(grid: Array<IntArray>, cells: Array<IntArray>, col: Int): Int {
    for (row in 0..C.boardRows) {
        if (wouldCollideAI(grid, cells, col, row)) {
            return row - 1
        }
    }
    return C.boardRows - 1
}

private fun wouldCollideAI(grid: Array<IntArray>, cells: Array<IntArray>, col: Int, row: Int): Boolean {
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

private fun placePiece(grid: Array<IntArray>, cells: Array<IntArray>, col: Int, row: Int, value: Int) {
    for (cell in cells) {
        val dr = cell[0]
        val dc = cell[1]
        val r = row + dr
        val c = col + dc
        if (r in 0 until C.boardRows && c in 0 until C.boardCols) {
            grid[r][c] = value
        }
    }
}

private fun countAndClearLines(grid: Array<IntArray>): Int {
    var cleared = 0
    var writeRow = C.boardRows - 1

    for (readRow in (C.boardRows - 1) downTo 0) {
        if (grid[readRow].all { it != 0 }) {
            cleared += 1
        } else {
            if (writeRow != readRow) {
                for (c in 0 until C.boardCols) {
                    grid[writeRow][c] = grid[readRow][c]
                }
            }
            writeRow -= 1
        }
    }

    for (row in writeRow downTo 0) {
        for (c in 0 until C.boardCols) {
            grid[row][c] = 0
        }
    }

    return cleared
}

/** Evaluate board state using El-Tetris weights */
private fun evaluate(grid: Array<IntArray>, linesCleared: Int): Double {
    val aggHeight = aggregateHeight(grid)
    val holes = countHoles(grid)
    val bump = bumpiness(grid)

    return (
        -0.510066 * aggHeight.toDouble() +
         0.760666 * linesCleared.toDouble() -
         0.356630 * holes.toDouble() -
         0.184483 * bump.toDouble()
    )
}

private fun aggregateHeight(grid: Array<IntArray>): Int {
    var total = 0
    for (col in 0 until C.boardCols) {
        for (row in 0 until C.boardRows) {
            if (grid[row][col] != 0) {
                total += C.boardRows - row
                break
            }
        }
    }
    return total
}

private fun countHoles(grid: Array<IntArray>): Int {
    var holes = 0
    for (col in 0 until C.boardCols) {
        var foundBlock = false
        for (row in 0 until C.boardRows) {
            if (grid[row][col] != 0) {
                foundBlock = true
            } else if (foundBlock) {
                holes += 1
            }
        }
    }
    return holes
}

private fun bumpiness(grid: Array<IntArray>): Int {
    val heights = mutableListOf<Int>()
    for (col in 0 until C.boardCols) {
        var h = 0
        for (row in 0 until C.boardRows) {
            if (grid[row][col] != 0) {
                h = C.boardRows - row
                break
            }
        }
        heights.add(h)
    }
    var bump = 0
    for (i in 0 until (heights.size - 1)) {
        bump += abs(heights[i] - heights[i + 1])
    }
    return bump
}

private fun copyGrid(grid: Array<IntArray>): Array<IntArray> {
    return Array(grid.size) { grid[it].copyOf() }
}
