package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class CollisionTest {

    // -- aabbOverlap --

    @Test
    fun testOverlappingBoxes() {
        val a = AABB(x = 0.0, y = 0.0, width = 20.0, height = 20.0)
        val b = AABB(x = 10.0, y = 10.0, width = 20.0, height = 20.0)
        assertTrue(aabbOverlap(a, b))
    }

    @Test
    fun testSeparatedBoxes() {
        val a = AABB(x = 0.0, y = 0.0, width = 10.0, height = 10.0)
        val b = AABB(x = 20.0, y = 20.0, width = 10.0, height = 10.0)
        assertFalse(aabbOverlap(a, b))
    }

    @Test
    fun testAdjacentBoxesNoOverlap() {
        val a = AABB(x = 0.0, y = 0.0, width = 10.0, height = 10.0)
        val b = AABB(x = 10.0, y = 0.0, width = 10.0, height = 10.0)
        assertFalse(aabbOverlap(a, b))
    }

    @Test
    fun testOnePixelOverlap() {
        val a = AABB(x = 0.0, y = 0.0, width = 10.0, height = 10.0)
        val b = AABB(x = 9.0, y = 0.0, width = 10.0, height = 10.0)
        assertTrue(aabbOverlap(a, b))
    }

    // -- getOverlap --

    @Test
    fun testGetOverlapReturnsNullForSeparated() {
        val a = AABB(x = 0.0, y = 0.0, width = 10.0, height = 10.0)
        val b = AABB(x = 50.0, y = 50.0, width = 10.0, height = 10.0)
        assertNull(getOverlap(a, b))
    }

    @Test
    fun testGetOverlapAmounts() {
        val a = AABB(x = 0.0, y = 0.0, width = 20.0, height = 20.0)
        val b = AABB(x = 15.0, y = 15.0, width = 20.0, height = 20.0)
        val r = getOverlap(a, b)!!
        assertEquals(5.0, r.overlapX, 0.001)
        assertEquals(5.0, r.overlapY, 0.001)
    }

    @Test
    fun testPushDirectionLeftOfB() {
        val a = AABB(x = 0.0, y = 0.0, width = 20.0, height = 20.0)
        val b = AABB(x = 15.0, y = 0.0, width = 20.0, height = 20.0)
        val r = getOverlap(a, b)!!
        assertEquals(-1.0, r.pushX, 0.001)
    }

    @Test
    fun testPushDirectionRightOfB() {
        val a = AABB(x = 15.0, y = 0.0, width = 20.0, height = 20.0)
        val b = AABB(x = 0.0, y = 0.0, width = 20.0, height = 20.0)
        val r = getOverlap(a, b)!!
        assertEquals(1.0, r.pushX, 0.001)
    }

    @Test
    fun testPushDirectionAboveB() {
        val a = AABB(x = 0.0, y = 0.0, width = 20.0, height = 20.0)
        val b = AABB(x = 0.0, y = 15.0, width = 20.0, height = 20.0)
        val r = getOverlap(a, b)!!
        assertEquals(-1.0, r.pushY, 0.001)
    }

    // -- playerAABB --

    @Test
    fun testPlayerAABBCreation() {
        val aabb = playerAABB(x = 10.0, y = 20.0, width = 30.0, height = 40.0)
        assertEquals(10.0, aabb.x, 0.001)
        assertEquals(20.0, aabb.y, 0.001)
        assertEquals(30.0, aabb.width, 0.001)
        assertEquals(40.0, aabb.height, 0.001)
    }
}
