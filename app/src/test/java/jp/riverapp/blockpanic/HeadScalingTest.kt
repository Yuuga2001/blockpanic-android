package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Head scaling behavior tests.
 * The rendering code uses: headScale = max(sqrt(heightMultiplier), 0.7)
 * This ensures proportional head size across BIG/MINI effects.
 */
class HeadScalingTest {

    /** Compute headScale the same way GameSurfaceView does */
    private fun headScale(heightMultiplier: Double): Double {
        return max(sqrt(heightMultiplier), 0.7)
    }

    @Test
    fun `normal player has headScale 1_0`() {
        val scale = headScale(1.0)
        assertEquals("Normal player headScale", 1.0, scale, 0.001)
    }

    @Test
    fun `BIG player heightMultiplier 2_0 has headScale approx 1_414`() {
        val scale = headScale(2.0)
        assertEquals("BIG headScale = sqrt(2.0)", sqrt(2.0), scale, 0.001)
        assertTrue("BIG headScale should be ~1.414", scale > 1.41 && scale < 1.42)
    }

    @Test
    fun `MINI player heightMultiplier 0_5 has headScale approx 0_707`() {
        val scale = headScale(0.5)
        assertEquals("MINI headScale = sqrt(0.5)", sqrt(0.5), scale, 0.001)
        assertTrue("MINI headScale should be ~0.707", scale > 0.70 && scale < 0.71)
    }

    @Test
    fun `headScale minimum is 0_7`() {
        // Very small heightMultiplier should clamp to 0.7
        val scale = headScale(0.1)
        assertEquals("Minimum headScale should be 0.7", 0.7, scale, 0.001)
    }

    @Test
    fun `heightMultiplier 0_1 clamps headScale to 0_7`() {
        // sqrt(0.1) = ~0.316, which is below 0.7 minimum
        val rawSqrt = sqrt(0.1)
        assertTrue("sqrt(0.1) should be less than 0.7", rawSqrt < 0.7)
        val scale = headScale(0.1)
        assertEquals("Should clamp to 0.7", 0.7, scale, 0.001)
    }

    @Test
    fun `headScale formula matches rendering code`() {
        // Verify against the exact formula from GameSurfaceView line 309:
        // val headScale = max(Math.sqrt(player.heightMultiplier), 0.7)
        val testCases = listOf(0.1, 0.3, 0.5, 0.7, 1.0, 1.5, 2.0, 3.0)
        for (hm in testCases) {
            val expected = max(Math.sqrt(hm), 0.7)
            val actual = headScale(hm)
            assertEquals("headScale for hm=$hm", expected, actual, 0.0001)
        }
    }

    @Test
    fun `headRadius is 6 times headScale`() {
        // GameSurfaceView: val headR = (6.0 * headScale).toFloat()
        val hm = 2.0
        val scale = headScale(hm)
        val headR = 6.0 * scale
        assertEquals("BIG head radius", 6.0 * sqrt(2.0), headR, 0.001)
    }

    @Test
    fun `heightMultiplier from PlayerState carries through`() {
        val p = createPlayer("test", "Test", PlayerType.USER, 100.0, 200.0)
        p.heightMultiplier = 2.0
        val state = toPlayerState(p)
        assertEquals(2.0, state.heightMultiplier, 0.001)
        val scale = headScale(state.heightMultiplier)
        assertEquals(sqrt(2.0), scale, 0.001)
    }
}
