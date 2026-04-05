package jp.riverapp.blockpanic

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * PlayerState cross-platform decoding tests.
 * Bug: Web version JSON lacks inputVx/activeEffect/effectEndTime fields,
 * causing decode failure -> state receive failure -> host disconnected.
 */
class PlayerStateDecodingTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun testDecodeWebVersionPlayerState() {
        val json = """
        {
            "id": "p-123",
            "name": "Player",
            "x": 100.0,
            "y": 200.0,
            "vx": 4.0,
            "vy": -12.0,
            "type": "user",
            "color": "blue",
            "alive": true,
            "onGround": true,
            "score": 42,
            "heightMultiplier": 1.0,
            "jumpMultiplier": 1.0
        }
        """
        val state = gson.fromJson(json, PlayerState::class.java)
        assertEquals("p-123", state.id)
        assertEquals(0.0, state.inputVx, 0.001)
        assertNull(state.activeEffect)
        assertEquals(0.0, state.effectEndTime, 0.001)
    }

    @Test
    fun testDecodeFullPlayerState() {
        val json = """
        {
            "id": "p-456",
            "name": "AndroidPlayer",
            "x": 50.0,
            "y": 100.0,
            "vx": 0.0,
            "vy": 0.0,
            "type": "cpu",
            "color": "black",
            "alive": true,
            "onGround": false,
            "score": 100,
            "heightMultiplier": 2.0,
            "jumpMultiplier": 1.5,
            "inputVx": 4.0,
            "activeEffect": "grow",
            "effectEndTime": 1000000.0
        }
        """
        val state = gson.fromJson(json, PlayerState::class.java)
        assertEquals(4.0, state.inputVx, 0.001)
        assertEquals(MysteryEffect.GROW, state.activeEffect)
        assertEquals(1000000.0, state.effectEndTime, 0.001)
    }

    @Test
    fun testDecodeWebStateMessage() {
        val json = """
        {
            "type": "state",
            "playerId": "p-123",
            "players": [
                {
                    "id": "p-123",
                    "name": "Host",
                    "x": 100, "y": 200,
                    "vx": 0, "vy": 0,
                    "type": "user", "color": "red",
                    "alive": true, "onGround": true,
                    "score": 10,
                    "heightMultiplier": 1, "jumpMultiplier": 1
                }
            ],
            "blocks": [],
            "coins": [],
            "mysteryItems": [],
            "roomElapsed": 30,
            "timestamp": 1234567890.0
        }
        """
        val msg = gson.fromJson(json, StateMessage::class.java)
        assertEquals("p-123", msg.playerId)
        assertEquals(1, msg.players.size)
        assertEquals(0.0, msg.players[0].inputVx, 0.001)
        assertNull(msg.players[0].activeEffect)
    }

    @Test
    fun testDecodeAllMysteryEffects() {
        for (effect in listOf("points", "shrink", "grow", "superjump")) {
            val json = """
            {
                "id": "p", "name": "P", "x": 0, "y": 0, "vx": 0, "vy": 0,
                "type": "user", "color": "blue", "alive": true, "onGround": true,
                "score": 0, "heightMultiplier": 1, "jumpMultiplier": 1,
                "activeEffect": "$effect"
            }
            """
            val state = gson.fromJson(json, PlayerState::class.java)
            assertNotNull("$effect should decode", state.activeEffect)
        }
    }

    @Test
    fun testDecodeNullActiveEffect() {
        val json = """
        {
            "id": "p", "name": "P", "x": 0, "y": 0, "vx": 0, "vy": 0,
            "type": "user", "color": "blue", "alive": true, "onGround": true,
            "score": 0, "heightMultiplier": 1, "jumpMultiplier": 1,
            "activeEffect": null
        }
        """
        val state = gson.fromJson(json, PlayerState::class.java)
        assertNull(state.activeEffect)
    }

    @Test
    fun testPlayerStateRoundTrip() {
        val original = PlayerState(
            id = "test", name = "Test", x = 100.0, y = 200.0, vx = 4.0, vy = -12.0,
            type = PlayerType.USER, color = PlayerColor.RED,
            alive = true, onGround = false, score = 50,
            heightMultiplier = 0.5, jumpMultiplier = 2.0,
            inputVx = -4.0, activeEffect = MysteryEffect.SHRINK, effectEndTime = 999.0
        )
        val jsonStr = gson.toJson(original)
        val decoded = gson.fromJson(jsonStr, PlayerState::class.java)
        assertEquals(original.id, decoded.id)
        assertEquals(original.inputVx, decoded.inputVx, 0.001)
        assertEquals(original.activeEffect, decoded.activeEffect)
        assertEquals(original.effectEndTime, decoded.effectEndTime, 0.001)
    }
}
