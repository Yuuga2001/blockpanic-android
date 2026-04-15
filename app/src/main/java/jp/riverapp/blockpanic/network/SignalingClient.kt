package jp.riverapp.blockpanic.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * REST signaling client for WebRTC room management.
 * Port from iOS SignalingClient.swift
 */
data class RoomInfo(
    val roomId: String,
    val hostName: String,
    val playerCount: Int,
    val createdAt: Double,
    val lastHeartbeat: Double = 0.0
)

data class SignalInfo(
    val peerId: String,
    val offer: String?,
    val answer: String?
)

data class LeaderboardRecord(
    val rank: Int,
    val playerName: String,
    val score: Int,
    val survivalTime: Int,
    val mode: String,
    val platform: String,
    val createdAt: Double,
    val isYou: Boolean = false
)

class SignalingError(val code: Int, message: String) : Exception("API error ($code): $message")

class SignalingClient {
    private val baseURL = "https://blockpanic.riverapp.jp/api"
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    // MARK: - Room Management

    suspend fun listRooms(): List<RoomInfo> {
        val data = get("$baseURL/rooms")
        val wrapper = gson.fromJson(data, RoomsResponse::class.java)
        return wrapper.rooms
    }

    suspend fun createRoom(hostName: String): String {
        val body = gson.toJson(mapOf("hostName" to hostName))
        val data = post("$baseURL/rooms", body)
        val result = gson.fromJson(data, CreateRoomResponse::class.java)
        return result.roomId
    }

    suspend fun updateRoom(roomId: String, playerCount: Int) {
        val body = gson.toJson(mapOf("playerCount" to playerCount))
        put("$baseURL/rooms/$roomId", body)
    }

    suspend fun deleteRoom(roomId: String) {
        delete("$baseURL/rooms/$roomId")
    }

    // MARK: - Signaling

    suspend fun postSignal(roomId: String, peerId: String, type: String, sdp: String) {
        val body = gson.toJson(mapOf("peerId" to peerId, "type" to type, "sdp" to sdp))
        post("$baseURL/rooms/$roomId/signal", body)
    }

    suspend fun getSignals(roomId: String): List<SignalInfo> {
        val data = get("$baseURL/rooms/$roomId/signal")
        val wrapper = gson.fromJson(data, SignalsResponse::class.java)
        return wrapper.signals
    }

    suspend fun getSignal(roomId: String, peerId: String): SignalInfo? {
        val encodedPeerId = java.net.URLEncoder.encode(peerId, "UTF-8")
        val data = get("$baseURL/rooms/$roomId/signal?peerId=$encodedPeerId")
        val wrapper = gson.fromJson(data, SignalResponse::class.java)
        return wrapper.signal
    }

    // MARK: - Leaderboard

    suspend fun getLeaderboard(deviceId: String): List<LeaderboardRecord> {
        val encoded = java.net.URLEncoder.encode(deviceId, "UTF-8")
        val data = get("$baseURL/leaderboard?deviceId=$encoded")
        val wrapper = gson.fromJson(data, LeaderboardResponse::class.java)
        return wrapper.records
    }

    suspend fun submitScore(score: Int, survivalTime: Int, playerName: String, mode: String, platform: String, deviceId: String) {
        val body = gson.toJson(mapOf(
            "score" to score, "survivalTime" to survivalTime,
            "playerName" to playerName, "mode" to mode,
            "platform" to platform, "deviceId" to deviceId
        ))
        post("$baseURL/leaderboard", body)
    }

    // MARK: - HTTP helpers

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        executeRequest(request)
    }

    private suspend fun post(url: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val body = jsonBody.toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()
        executeRequest(request)
    }

    private suspend fun put(url: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val body = jsonBody.toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).put(body).build()
        executeRequest(request)
    }

    private suspend fun delete(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).delete().build()
        executeRequest(request)
    }

    private suspend fun executeRequest(request: Request): String = suspendCoroutine { cont ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (response.code >= 400) {
                    val errorMsg = try {
                        val json = gson.fromJson(body, Map::class.java)
                        json["error"]?.toString() ?: "Unknown"
                    } catch (_: Exception) { "Unknown" }
                    cont.resumeWithException(SignalingError(response.code, errorMsg))
                } else {
                    cont.resume(body)
                }
            }
        })
    }

    // Response types
    private data class RoomsResponse(val rooms: List<RoomInfo>)
    private data class CreateRoomResponse(val roomId: String)
    private data class SignalsResponse(val signals: List<SignalInfo>)
    private data class SignalResponse(val signal: SignalInfo?)
    private data class LeaderboardResponse(val records: List<LeaderboardRecord>)
}
