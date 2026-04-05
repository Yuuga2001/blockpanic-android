package jp.riverapp.blockpanic.engine

// Tetromino piece definitions -- 1:1 port from shared/game/TetrominoPieces.ts
// 7 pieces x 4 rotations, each rotation = array of (row, col) offsets

val pieceRotations: Array<Array<Array<IntArray>>> = arrayOf(
    // 0: I
    arrayOf(
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(0,2), intArrayOf(0,3)),
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(2,0), intArrayOf(3,0)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(0,2), intArrayOf(0,3)),
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(2,0), intArrayOf(3,0))
    ),
    // 1: O
    arrayOf(
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,0), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,0), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,0), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,0), intArrayOf(1,1))
    ),
    // 2: T
    arrayOf(
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(0,2), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(2,0), intArrayOf(1,1)),
        arrayOf(intArrayOf(1,0), intArrayOf(1,1), intArrayOf(1,2), intArrayOf(0,1)),
        arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,0))
    ),
    // 3: S
    arrayOf(
        arrayOf(intArrayOf(0,1), intArrayOf(0,2), intArrayOf(1,0), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(1,1), intArrayOf(2,1)),
        arrayOf(intArrayOf(0,1), intArrayOf(0,2), intArrayOf(1,0), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(1,1), intArrayOf(2,1))
    ),
    // 4: Z
    arrayOf(
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,1), intArrayOf(1,2)),
        arrayOf(intArrayOf(0,1), intArrayOf(1,0), intArrayOf(1,1), intArrayOf(2,0)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,1), intArrayOf(1,2)),
        arrayOf(intArrayOf(0,1), intArrayOf(1,0), intArrayOf(1,1), intArrayOf(2,0))
    ),
    // 5: L
    arrayOf(
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(2,0), intArrayOf(2,1)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(0,2), intArrayOf(1,0)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1)),
        arrayOf(intArrayOf(1,0), intArrayOf(1,1), intArrayOf(1,2), intArrayOf(0,2))
    ),
    // 6: J
    arrayOf(
        arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(2,0)),
        arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(1,1), intArrayOf(1,2)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(1,0), intArrayOf(2,0)),
        arrayOf(intArrayOf(0,0), intArrayOf(0,1), intArrayOf(0,2), intArrayOf(1,2))
    )
)

fun pieceWidth(cells: Array<IntArray>): Int {
    return (cells.maxOfOrNull { it[1] } ?: 0) + 1
}

fun pieceHeight(cells: Array<IntArray>): Int {
    return (cells.maxOfOrNull { it[0] } ?: 0) + 1
}
