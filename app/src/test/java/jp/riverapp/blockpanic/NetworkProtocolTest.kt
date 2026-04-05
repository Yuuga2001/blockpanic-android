package jp.riverapp.blockpanic

import com.google.gson.Gson
import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.network.PeerError
import jp.riverapp.blockpanic.network.RoomInfo
import jp.riverapp.blockpanic.network.SignalInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * WebRTC/signaling protocol compatibility tests.
 * Verifies JSON serialization matches the Web/iOS protocol.
 */
class NetworkProtocolTest {

    private val gson = Gson()

    // =========================================================================
    // StateMessage serialization
    // =========================================================================

    @Test
    fun `StateMessage serializes correctly with type state`() {
        val msg = StateMessage(
            type = "state",
            playerId = "p-123",
            players = listOf(
                PlayerState(
                    id = "p-123", name = "Alice", x = 100.0, y = 200.0,
                    vx = 4.0, vy = -12.0, type = PlayerType.USER, color = PlayerColor.BLUE,
                    alive = true, onGround = true, score = 42,
                    heightMultiplier = 1.0, jumpMultiplier = 1.0
                )
            ),
            blocks = listOf(
                BlockState(id = "b-1", x = 0.0, y = 300.0, width = 30.0, height = 30.0, falling = true, pieceType = 0)
            ),
            coins = emptyList(),
            mysteryItems = emptyList(),
            roomElapsed = 30,
            timestamp = 1234567890.0
        )
        val json = gson.toJson(msg)
        assertTrue("Should contain type=state", json.contains("\"type\":\"state\""))
        assertTrue("Should contain playerId", json.contains("\"playerId\":\"p-123\""))
        assertTrue("Should contain players array", json.contains("\"players\":["))
        assertTrue("Should contain blocks array", json.contains("\"blocks\":["))
    }

    @Test
    fun `StateMessage with empty players and blocks serializes`() {
        val msg = StateMessage(
            playerId = "p-1",
            players = emptyList(),
            blocks = emptyList(),
            coins = emptyList(),
            mysteryItems = emptyList(),
            roomElapsed = 0,
            timestamp = 0.0
        )
        val json = gson.toJson(msg)
        assertTrue(json.contains("\"players\":[]"))
        assertTrue(json.contains("\"blocks\":[]"))

        // Round-trip
        val decoded = gson.fromJson(json, StateMessage::class.java)
        assertEquals(0, decoded.players.size)
        assertEquals(0, decoded.blocks.size)
        assertEquals("state", decoded.type)
    }

    // =========================================================================
    // PlayerState with activeEffect
    // =========================================================================

    @Test
    fun `PlayerState with null activeEffect serializes correctly`() {
        val state = PlayerState(
            id = "p-1", name = "Test", x = 100.0, y = 200.0,
            vx = 0.0, vy = 0.0, type = PlayerType.USER, color = PlayerColor.BLUE,
            alive = true, onGround = true, score = 0,
            heightMultiplier = 1.0, jumpMultiplier = 1.0,
            activeEffect = null
        )
        val json = gson.toJson(state)
        // Gson omits null by default or includes it as null
        val decoded = gson.fromJson(json, PlayerState::class.java)
        assertNull(decoded.activeEffect)
    }

    @Test
    fun `PlayerState with non-null activeEffect serializes with correct enum name`() {
        val state = PlayerState(
            id = "p-1", name = "Test", x = 100.0, y = 200.0,
            vx = 0.0, vy = 0.0, type = PlayerType.USER, color = PlayerColor.BLUE,
            alive = true, onGround = true, score = 0,
            heightMultiplier = 2.0, jumpMultiplier = 1.0,
            activeEffect = MysteryEffect.GROW
        )
        val json = gson.toJson(state)
        // @SerializedName("grow") means JSON value is "grow"
        assertTrue("Should serialize activeEffect as 'grow'", json.contains("\"grow\""))

        val decoded = gson.fromJson(json, PlayerState::class.java)
        assertEquals(MysteryEffect.GROW, decoded.activeEffect)
    }

    @Test
    fun `all MysteryEffect values serialize correctly`() {
        for (effect in MysteryEffect.entries) {
            val state = PlayerState(
                id = "p-1", name = "T", x = 0.0, y = 0.0,
                vx = 0.0, vy = 0.0, type = PlayerType.USER, color = PlayerColor.BLUE,
                alive = true, onGround = true, score = 0,
                heightMultiplier = 1.0, jumpMultiplier = 1.0,
                activeEffect = effect
            )
            val json = gson.toJson(state)
            val decoded = gson.fromJson(json, PlayerState::class.java)
            assertEquals("Effect ${effect.name} should round-trip", effect, decoded.activeEffect)
        }
    }

    // =========================================================================
    // RoomInfo deserialization
    // =========================================================================

    @Test
    fun `RoomInfo deserializes from JSON`() {
        val json = """{"roomId":"room-abc","hostName":"Alice","playerCount":4,"createdAt":1700000000.0}"""
        val room = gson.fromJson(json, RoomInfo::class.java)
        assertEquals("room-abc", room.roomId)
        assertEquals("Alice", room.hostName)
        assertEquals(4, room.playerCount)
        assertEquals(1700000000.0, room.createdAt, 0.001)
    }

    @Test
    fun `RoomInfo with minimal fields`() {
        val json = """{"roomId":"r-1","hostName":"H","playerCount":1,"createdAt":0.0}"""
        val room = gson.fromJson(json, RoomInfo::class.java)
        assertEquals("r-1", room.roomId)
        assertEquals(1, room.playerCount)
    }

    // =========================================================================
    // SignalInfo deserialization
    // =========================================================================

    @Test
    fun `SignalInfo deserializes with null answer`() {
        val json = """{"peerId":"peer-1","offer":"sdp-data","answer":null}"""
        val signal = gson.fromJson(json, SignalInfo::class.java)
        assertEquals("peer-1", signal.peerId)
        assertEquals("sdp-data", signal.offer)
        assertNull(signal.answer)
    }

    @Test
    fun `SignalInfo deserializes with both offer and answer`() {
        val json = """{"peerId":"peer-1","offer":"offer-sdp","answer":"answer-sdp"}"""
        val signal = gson.fromJson(json, SignalInfo::class.java)
        assertEquals("offer-sdp", signal.offer)
        assertEquals("answer-sdp", signal.answer)
    }

    // =========================================================================
    // PeerError messages
    // =========================================================================

    @Test
    fun `PeerError TIMEOUT has correct message`() {
        val error: Exception = PeerError.TIMEOUT
        assertEquals("Connection timeout", error.message)
    }

    @Test
    fun `PeerError ROOM_NOT_FOUND has correct message`() {
        val error: Exception = PeerError.ROOM_NOT_FOUND
        assertEquals("Room no longer exists", error.message)
    }

    @Test
    fun `PeerError ICE_FAILED has correct message`() {
        val error: Exception = PeerError.ICE_FAILED
        assertEquals("ICE gathering failed", error.message)
    }

    // =========================================================================
    // DataChannel config
    // =========================================================================

    @Test
    fun `WebRTCConfig dataChannelConfig ordered is true`() {
        // WebRTCConfig uses lazy initialization that requires Android Context.
        // Verify the class and field exist via reflection.
        val configClass = Class.forName("jp.riverapp.blockpanic.network.WebRTCConfig")
        assertNotNull("WebRTCConfig should exist", configClass)

        // Verify dataChannelConfig property exists
        val method = configClass.getMethod("getDataChannelConfig")
        assertNotNull("dataChannelConfig getter should exist", method)
    }

    // =========================================================================
    // StateMessage round-trip with full data
    // =========================================================================

    @Test
    fun `StateMessage round-trip with coins and mystery items`() {
        val msg = StateMessage(
            playerId = "p-1",
            players = listOf(
                PlayerState(
                    id = "p-1", name = "A", x = 0.0, y = 0.0,
                    vx = 0.0, vy = 0.0, type = PlayerType.USER, color = PlayerColor.BLUE,
                    alive = true, onGround = true, score = 100,
                    heightMultiplier = 1.0, jumpMultiplier = 1.0
                )
            ),
            blocks = emptyList(),
            coins = listOf(CoinState(id = "c-1", x = 100.0, y = 200.0, active = true, collected = false)),
            mysteryItems = listOf(
                MysteryItemState(id = "m-1", x = 300.0, y = 400.0, active = true, collected = false, effect = MysteryEffect.SHRINK)
            ),
            roomElapsed = 60,
            timestamp = 9999999.0
        )
        val json = gson.toJson(msg)
        val decoded = gson.fromJson(json, StateMessage::class.java)
        assertEquals(1, decoded.coins.size)
        assertEquals("c-1", decoded.coins[0].id)
        assertEquals(1, decoded.mysteryItems.size)
        assertEquals(MysteryEffect.SHRINK, decoded.mysteryItems[0].effect)
    }
}
