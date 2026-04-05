package jp.riverapp.blockpanic.engine

// 1:1 port from shared/game/TetrisSystem.ts

class TetrisSystem {
    private val boards: List<TetrisBoard>

    init {
        // 3 boards with staggered spawn timers
        boards = (0 until C.boardCount).map { i ->
            TetrisBoard(boardIndex = i, initialDelay = i.toDouble() * 1000.0)
        }
    }

    fun update(dtMs: Double) {
        for (board in boards) {
            board.update(dtMs)
        }
    }

    fun getBlocks(): List<BlockState> {
        val allBlocks = mutableListOf<BlockState>()
        for (i in boards.indices) {
            val xOffset = i.toDouble() * C.boardWidth
            allBlocks.addAll(boards[i].getBlocks(xOffset))
        }
        return allBlocks
    }

    fun getLandedBlocks(): List<BlockState> {
        return getBlocks().filter { !it.falling }
    }

    fun getAllBlocks(): List<BlockState> {
        return getBlocks()
    }

    fun reset() {
        for (board in boards) {
            board.reset()
        }
    }
}
