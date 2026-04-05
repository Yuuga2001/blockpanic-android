package jp.riverapp.blockpanic

import com.google.gson.Gson
import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class CoinMysterySystemTest {

    private val gson = Gson()

    // -- Coin spawn --

    @Test
    fun testCoinSpawnsAfterInterval() {
        val gs = GameState()
        gs.addPlayer(id = "p1", name = "Test")

        val tickCount = (C.coinSpawnInterval / C.tickMs).toInt() + 1
        for (i in 0 until tickCount) {
            gs.tick(dtMs = C.tickMs)
        }

        val state = gs.getWorldState()
        assertTrue("Coins should spawn after interval", state.coins.isNotEmpty())
    }

    // -- CoinState decode --

    @Test
    fun testCoinStateDecode() {
        val json = """{"id": "coin-1", "x": 100.0, "y": 200.0, "active": true, "collected": false}"""
        val coin = gson.fromJson(json, CoinState::class.java)
        assertEquals("coin-1", coin.id)
        assertTrue(coin.active)
        assertFalse(coin.collected)
    }

    // -- MysteryItemState decode --

    @Test
    fun testMysteryItemStateDecode() {
        val json = """{"id": "m-1", "x": 50.0, "y": 100.0, "active": true, "collected": false, "effect": null}"""
        val item = gson.fromJson(json, MysteryItemState::class.java)
        assertEquals("m-1", item.id)
        assertNull(item.effect)
    }

    @Test
    fun testMysteryItemWithEffectDecode() {
        val json = """{"id": "m-2", "x": 50.0, "y": 100.0, "active": false, "collected": true, "effect": "grow"}"""
        val item = gson.fromJson(json, MysteryItemState::class.java)
        assertEquals(MysteryEffect.GROW, item.effect)
        assertTrue(item.collected)
    }

    // -- BlockState decode --

    @Test
    fun testBlockStateDecode() {
        val json = """{"id": "b-1", "x": 90.0, "y": 0.0, "width": 30.0, "height": 30.0, "falling": true, "pieceType": 2}"""
        val block = gson.fromJson(json, BlockState::class.java)
        assertEquals("b-1", block.id)
        assertTrue(block.falling)
        assertEquals(2, block.pieceType)
    }

    // -- StateMessage decode --

    @Test
    fun testFullStateMessageDecode() {
        val json = """
        {
            "type": "state",
            "playerId": "p-1",
            "players": [],
            "blocks": [],
            "coins": [{"id": "c1", "x": 100, "y": 200, "active": true, "collected": false}],
            "mysteryItems": [{"id": "m1", "x": 50, "y": 100, "active": true, "collected": false, "effect": null}],
            "roomElapsed": 60,
            "timestamp": 1234567890.0
        }
        """
        val msg = gson.fromJson(json, StateMessage::class.java)
        assertEquals(1, msg.coins.size)
        assertEquals(1, msg.mysteryItems.size)
        assertEquals(60, msg.roomElapsed)
    }
}
