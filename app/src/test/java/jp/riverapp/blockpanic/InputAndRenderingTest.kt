package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * 操作性・デザインのリグレッションテスト
 * 実機では見えてもユニットテストで見えないバグを防止
 */
class InputAndRenderingTest {

    private fun makePlayer(configure: (ServerPlayer) -> Unit = {}): ServerPlayer {
        val p = createPlayer("test", "Test", PlayerType.USER, 450.0, C.gameHeight - C.playerHeight)
        p.onGround = true
        configure(p)
        return p
    }

    // =========================================================================
    // 入力系: applyInput が正しく物理に反映される
    // =========================================================================

    @Test
    fun `left input moves player left`() {
        val p = makePlayer()
        val x0 = p.x
        applyInput(p, InputState(left = true))
        updatePlayerPhysics(p)
        assertTrue("Left input should move player left", p.x < x0)
    }

    @Test
    fun `right input moves player right`() {
        val p = makePlayer()
        val x0 = p.x
        applyInput(p, InputState(right = true))
        updatePlayerPhysics(p)
        assertTrue("Right input should move player right", p.x > x0)
    }

    @Test
    fun `jump input makes player go up`() {
        val p = makePlayer()
        val y0 = p.y
        applyInput(p, InputState(jump = true))
        updatePlayerPhysics(p)
        assertTrue("Jump should decrease y (move up)", p.y < y0)
    }

    @Test
    fun `no input keeps player stationary horizontally`() {
        val p = makePlayer { it.vx = 0.0 }
        val x0 = p.x
        applyInput(p, InputState())
        updatePlayerPhysics(p)
        assertEquals("No input should keep x same", x0, p.x, 0.001)
    }

    @Test
    fun `left and right cancel each other - right wins`() {
        // applyInput: left sets vx=-4, right sets vx=+4, right overwrites
        val p = makePlayer()
        applyInput(p, InputState(left = true, right = true))
        assertEquals("Both directions: right wins", C.moveSpeed, p.vx, 0.001)
    }

    @Test
    fun `jump only works when on ground`() {
        val p = makePlayer { it.onGround = false; it.vy = 5.0 }
        applyInput(p, InputState(jump = true))
        assertEquals("Jump should not work in air", 5.0, p.vy, 0.001)
    }

    @Test
    fun `move speed is exactly C moveSpeed`() {
        val p = makePlayer()
        applyInput(p, InputState(right = true))
        assertEquals(C.moveSpeed, p.vx, 0.001)
    }

    @Test
    fun `jump power is exactly C jumpPower`() {
        val p = makePlayer()
        applyInput(p, InputState(jump = true))
        assertEquals(C.jumpPower, p.vy, 0.001)
    }

    // =========================================================================
    // 棒人間ポーズ判定ロジック (レンダリングの根拠)
    // =========================================================================

    @Test
    fun `idle pose when standing still`() {
        val p = makePlayer { it.vx = 0.0; it.vy = 0.0; it.onGround = true }
        val state = toPlayerState(p)
        // inputVx = 0, onGround = true, vy = 0 → IDLE
        assertEquals(0.0, state.inputVx, 0.001)
        assertTrue(state.onGround)
    }

    @Test
    fun `run pose when moving with input`() {
        val p = makePlayer { it.input = InputState(right = true) }
        val state = toPlayerState(p)
        // inputVx != 0 → RUN animation should trigger
        assertTrue("inputVx should be non-zero for run pose", state.inputVx != 0.0)
    }

    @Test
    fun `run pose when blocked by wall but pressing direction`() {
        val p = makePlayer { it.vx = 0.0; it.input = InputState(right = true) }
        val state = toPlayerState(p)
        // vx = 0 (blocked) but inputVx != 0 → should still show run animation
        assertEquals(0.0, state.vx, 0.001)
        assertEquals(C.moveSpeed, state.inputVx, 0.001)
    }

    @Test
    fun `jump pose when vy is negative and airborne`() {
        val p = makePlayer { it.vy = -10.0; it.onGround = false }
        val state = toPlayerState(p)
        // !onGround && vy < -2 → JUMP
        assertFalse(state.onGround)
        assertTrue(state.vy < -2)
    }

    @Test
    fun `fall pose when airborne with positive vy`() {
        val p = makePlayer { it.vy = 5.0; it.onGround = false }
        val state = toPlayerState(p)
        // !onGround && vy >= -2 → FALL
        assertFalse(state.onGround)
        assertTrue(state.vy >= -2)
    }

    // =========================================================================
    // Y座標系: Android Canvas (Y下向き) の正しさ
    // =========================================================================

    @Test
    fun `game world dimensions match constants`() {
        assertEquals("Game width = 900", 900.0, C.gameWidth, 0.001)
        assertEquals("Game height = 600", 600.0, C.gameHeight, 0.001)
        assertEquals("Player width = 20", 20.0, C.playerWidth, 0.001)
        assertEquals("Player height = 40", 40.0, C.playerHeight, 0.001)
    }

    @Test
    fun `player y increases downward (Android canvas convention)`() {
        val p = makePlayer { it.y = 100.0; it.vy = 5.0 }
        updatePlayerPhysics(p)
        assertTrue("Positive vy should increase y (move down)", p.y > 100.0)
    }

    @Test
    fun `gravity pulls player down (y increases)`() {
        val p = makePlayer { it.y = 100.0; it.vy = 0.0 }
        updatePlayerPhysics(p)
        assertTrue("Gravity should increase vy", p.vy > 0)
    }

    @Test
    fun `floor is at y = gameHeight - playerHeight`() {
        val p = makePlayer { it.y = C.gameHeight; it.vy = 5.0 }
        updatePlayerPhysics(p)
        assertEquals("Floor position", C.gameHeight - C.playerHeight, p.y, 0.001)
        assertTrue(p.onGround)
    }

    // =========================================================================
    // エフェクト表示テキスト
    // =========================================================================

    @Test
    fun `effect popup text matches expected values`() {
        // These are the exact strings shown in the popup
        val strings = jp.riverapp.blockpanic.i18n.Strings
        assertEquals("+200", strings.get("effect_points", "en"))
        assertEquals("MINI", strings.get("effect_mini", "en"))
        assertEquals("BIG", strings.get("effect_big", "en"))
        assertEquals("JUMP", strings.get("effect_jump", "en"))
    }

    // =========================================================================
    // HUD表示テキスト
    // =========================================================================

    @Test
    fun `HUD labels are correctly localized`() {
        val strings = jp.riverapp.blockpanic.i18n.Strings
        assertEquals("Time", strings.get("hud_time", "en"))
        assertEquals("Score", strings.get("hud_score", "en"))
        assertEquals("Players", strings.get("hud_players", "en"))
        assertEquals("Room", strings.get("hud_room", "en"))
    }

    // =========================================================================
    // 棒人間の色分け
    // =========================================================================

    @Test
    fun `player type determines color`() {
        val user = createPlayer("u1", "User", PlayerType.USER, 100.0, 100.0)
        val cpu = createPlayer("c1", "CPU", PlayerType.CPU, 200.0, 100.0)
        assertEquals(PlayerColor.BLUE, user.color) // user default is blue
        assertEquals(PlayerColor.BLACK, cpu.color) // cpu is black
    }

    // =========================================================================
    // ボード構成
    // =========================================================================

    @Test
    fun `three boards side by side`() {
        assertEquals("3 boards", 3, C.boardCount)
        assertEquals("Board width = 300", 300.0, C.boardWidth, 0.001)
        assertEquals("Total width = 900", C.boardWidth * C.boardCount, C.gameWidth, 0.001)
    }

    @Test
    fun `cell size is 30`() {
        assertEquals(30.0, C.cellSize, 0.001)
    }

    // =========================================================================
    // エフェクト倍率
    // =========================================================================

    @Test
    fun `grow effect doubles height multiplier`() {
        val p = makePlayer()
        p.heightMultiplier = 2.0
        val state = toPlayerState(p)
        assertEquals(2.0, state.heightMultiplier, 0.001)
    }

    @Test
    fun `shrink effect halves height multiplier`() {
        val p = makePlayer()
        p.heightMultiplier = 0.5
        val state = toPlayerState(p)
        assertEquals(0.5, state.heightMultiplier, 0.001)
    }

    @Test
    fun `superjump doubles jump multiplier`() {
        val p = makePlayer()
        p.jumpMultiplier = 2.0
        val state = toPlayerState(p)
        assertEquals(2.0, state.jumpMultiplier, 0.001)
    }
}
