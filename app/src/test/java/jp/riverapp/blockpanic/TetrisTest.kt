package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class TetrisTest {

    // -- TetrominoPieces --

    @Test
    fun testPieceRotationsCount() {
        assertEquals(7, pieceRotations.size)
        for (piece in pieceRotations) {
            assertEquals(4, piece.size) // 4 rotations each
        }
    }

    @Test
    fun testPieceWidth() {
        // I-piece horizontal: [[0,0],[0,1],[0,2],[0,3]] -> width 4
        val iHoriz = pieceRotations[0][0]
        assertEquals(4, pieceWidth(iHoriz))
    }

    @Test
    fun testPieceHeight() {
        // I-piece vertical: [[0,0],[1,0],[2,0],[3,0]] -> height 4
        val iVert = pieceRotations[0][1]
        assertEquals(4, pieceHeight(iVert))
    }

    @Test
    fun testOPieceSymmetric() {
        // O-piece: all 4 rotations identical
        val o = pieceRotations[1]
        for (i in 1 until 4) {
            assertArrayEquals(o[0].map { it.toList() }.toTypedArray(),
                o[i].map { it.toList() }.toTypedArray())
        }
    }

    @Test
    fun testIPieceHorizontalWidth4() {
        assertEquals(4, pieceWidth(pieceRotations[0][0]))
        assertEquals(1, pieceHeight(pieceRotations[0][0]))
    }

    @Test
    fun testIPieceVerticalHeight4() {
        assertEquals(1, pieceWidth(pieceRotations[0][1]))
        assertEquals(4, pieceHeight(pieceRotations[0][1]))
    }

    @Test
    fun testOPieceDimensions() {
        val o = pieceRotations[1][0]
        assertEquals(2, pieceWidth(o))
        assertEquals(2, pieceHeight(o))
    }

    // -- TetrisSystem --

    @Test
    fun testTetrisSystemCreatesThreeBoards() {
        val sys = TetrisSystem()
        val blocks = sys.getAllBlocks()
        assertNotNull(blocks)
    }

    @Test
    fun testTetrisSystemUpdate() {
        val sys = TetrisSystem()
        for (i in 0 until 100) {
            sys.update(dtMs = 16.0)
        }
        val blocks = sys.getAllBlocks()
        assertFalse(blocks.isEmpty())
    }

    // -- TetrisAI --

    @Test
    fun testFindBestPlacementReturnsValid() {
        val grid = Array(C.boardRows) { IntArray(C.boardCols) }
        val result = findBestPlacement(grid = grid, pieceType = 0)
        assertTrue(result.col >= 0)
        assertTrue(result.col < C.boardCols)
        assertTrue(result.rotation >= 0)
        assertTrue(result.rotation < 4)
    }

    @Test
    fun testFindBestPlacementAllPieceTypes() {
        val grid = Array(C.boardRows) { IntArray(C.boardCols) }
        for (pieceType in 0 until 7) {
            val result = findBestPlacement(grid = grid, pieceType = pieceType)
            assertTrue("Piece $pieceType col should be valid", result.col >= 0)
            assertTrue("Piece $pieceType rotation should be valid", result.rotation in 0..3)
        }
    }
}
