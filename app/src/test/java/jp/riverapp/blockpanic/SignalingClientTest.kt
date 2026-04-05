package jp.riverapp.blockpanic

import com.google.gson.Gson
import jp.riverapp.blockpanic.network.RoomInfo
import jp.riverapp.blockpanic.network.SignalInfo
import jp.riverapp.blockpanic.network.SignalingError
import org.junit.Assert.*
import org.junit.Test

class SignalingClientTest {

    private val gson = Gson()

    // -- RoomInfo decode --

    @Test
    fun testRoomInfoDecode() {
        val json = """{"roomId": "room-123", "hostName": "Alice", "playerCount": 3, "createdAt": 1700000000.0}"""
        val room = gson.fromJson(json, RoomInfo::class.java)
        assertEquals("room-123", room.roomId)
        assertEquals("Alice", room.hostName)
        assertEquals(3, room.playerCount)
    }

    // -- SignalInfo decode --

    @Test
    fun testSignalInfoWithOffer() {
        val json = """{"peerId": "peer-1", "offer": "sdp-offer-data", "answer": null}"""
        val signal = gson.fromJson(json, SignalInfo::class.java)
        assertEquals("peer-1", signal.peerId)
        assertEquals("sdp-offer-data", signal.offer)
        assertNull(signal.answer)
    }

    @Test
    fun testSignalInfoWithAnswer() {
        val json = """{"peerId": "peer-1", "offer": "sdp-offer", "answer": "sdp-answer"}"""
        val signal = gson.fromJson(json, SignalInfo::class.java)
        assertEquals("sdp-answer", signal.answer)
    }

    // -- SignalingError --

    @Test
    fun testSignalingErrorContainsCode() {
        val error = SignalingError(500, "Internal error")
        assertTrue(error.message!!.contains("500"))
        assertTrue(error.message!!.contains("Internal error"))
    }

    @Test
    fun testSignalingErrorIs4xx() {
        val error = SignalingError(404, "Not found")
        assertTrue(error.message!!.contains("404"))
    }
}
