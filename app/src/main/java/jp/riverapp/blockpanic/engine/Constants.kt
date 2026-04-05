package jp.riverapp.blockpanic.engine

// Block Panic Game Constants -- 1:1 port from shared/types.ts

object C {
    // Grid / Board
    const val cellSize: Double = 30.0
    const val boardCols: Int = 10
    const val boardRows: Int = 20
    const val boardCount: Int = 3
    val boardWidth: Double = boardCols.toDouble() * cellSize   // 300
    val gameWidth: Double = boardWidth * boardCount.toDouble()  // 900
    val gameHeight: Double = boardRows.toDouble() * cellSize    // 600

    // Player
    const val playerWidth: Double = 20.0
    const val playerHeight: Double = 40.0

    // Physics
    const val gravity: Double = 0.7
    const val jumpPower: Double = -12.0
    const val moveSpeed: Double = 4.0
    const val maxFallSpeed: Double = 15.0

    // Timing
    const val tickRate: Int = 60
    val tickMs: Double = 1000.0 / tickRate.toDouble() // ~16.67
    const val broadcastRate: Int = 20
    val broadcastMs: Double = 1000.0 / broadcastRate.toDouble() // 50

    // Players
    const val maxPlayers: Int = 30
    const val minPlayers: Int = 5
    const val maxRoomPlayers: Int = 10

    // CPU
    const val cpuUpdateInterval: Double = 500.0 // ms

    // Tetris
    const val blockFallSpeed: Double = 1.0     // cells/sec initial
    const val blockSpawnInterval: Double = 3000.0 // ms

    // Coin
    const val coinRadius: Double = 12.0
    const val coinSpawnInterval: Double = 10000.0
    const val coinLifetime: Double = 10000.0
    const val coinScore: Int = 100
    const val coinFallSpeed: Double = 3.0

    // Mystery Item
    const val mysteryRadius: Double = 12.0
    const val mysterySpawnInterval: Double = 10000.0
    const val mysterySpawnOffset: Double = 5000.0
    const val mysteryLifetime: Double = 10000.0
    const val mysteryFallSpeed: Double = 3.0
    const val mysteryEffectDuration: Double = 10000.0
}
