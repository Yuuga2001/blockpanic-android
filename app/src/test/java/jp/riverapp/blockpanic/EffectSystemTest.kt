package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class EffectSystemTest {

    // -- Effect application --

    @Test
    fun testPointsEffectAddsScore() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        val scoreBefore = player.coinScore
        player.coinScore += 200
        player.activeEffect = MysteryEffect.POINTS
        assertEquals(scoreBefore + 200, player.coinScore)
    }

    @Test
    fun testShrinkEffectReducesHeight() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.heightMultiplier = 0.5
        player.activeEffect = MysteryEffect.SHRINK
        assertEquals(0.5, player.heightMultiplier, 0.001)
        val state = toPlayerState(player)
        assertEquals(MysteryEffect.SHRINK, state.activeEffect)
    }

    @Test
    fun testGrowEffectIncreasesHeight() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.heightMultiplier = 2.0
        player.activeEffect = MysteryEffect.GROW
        assertEquals(2.0, player.heightMultiplier, 0.001)
    }

    @Test
    fun testSuperjumpEffect() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.jumpMultiplier = 2.0
        player.activeEffect = MysteryEffect.SUPERJUMP
        assertEquals(2.0, player.jumpMultiplier, 0.001)
    }

    // -- toPlayerState includes effect info --

    @Test
    fun testPlayerStateIncludesActiveEffect() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.activeEffect = MysteryEffect.GROW
        player.effectEndTime = 999999.0
        val state = toPlayerState(player)
        assertEquals(MysteryEffect.GROW, state.activeEffect)
        assertEquals(999999.0, state.effectEndTime, 0.001)
    }

    @Test
    fun testPlayerStateWithNoEffect() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        val state = toPlayerState(player)
        assertNull(state.activeEffect)
        assertEquals(0.0, state.effectEndTime, 0.001)
    }

    // -- inputVx --

    @Test
    fun testPlayerStateIncludesInputVx() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.input = InputState(left = true)
        val state = toPlayerState(player)
        assertEquals(-C.moveSpeed, state.inputVx, 0.001)
    }

    @Test
    fun testPlayerStateInputVxRight() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.input = InputState(right = true)
        val state = toPlayerState(player)
        assertEquals(C.moveSpeed, state.inputVx, 0.001)
    }

    @Test
    fun testPlayerStateInputVxNone() {
        val gs = GameState()
        val player = gs.addPlayer(id = "test", name = "Test")
        player.input = InputState()
        val state = toPlayerState(player)
        assertEquals(0.0, state.inputVx, 0.001)
    }
}
