package jp.riverapp.blockpanic.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jp.riverapp.blockpanic.i18n.L
import java.text.SimpleDateFormat
import java.util.*

/**
 * Game record model + persistence.
 * Port from iOS GameRecord.swift
 */
data class GameRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val survivalTime: Int,     // sec
    val score: Int,            // point
    val mode: String,          // "Single" / "Online(Host)" / "Online(Member)"
    val playerName: String
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            return sdf.format(Date(date))
        }
}

object GameRecordStore {
    private const val KEY = "gameRecords"
    private val gson = Gson()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("blockpanic_records", Context.MODE_PRIVATE)
    }

    fun load(): List<GameRecord> {
        val json = prefs?.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<GameRecord>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(record: GameRecord) {
        val records = load().toMutableList()
        records.add(0, record)
        prefs?.edit()?.putString(KEY, gson.toJson(records))?.apply()
    }

    val deviceId: String
        get() {
            val key = "blockpanic_device_id"
            var id = prefs?.getString(key, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs?.edit()?.putString(key, id)?.apply()
            }
            return id
        }

    fun clear() {
        prefs?.edit()?.remove(KEY)?.apply()
    }

    fun modeString(mode: GameMode): String = when (mode) {
        GameMode.LOCAL -> L("mode_single")
        GameMode.P2P_HOST -> L("mode_host")
        GameMode.P2P_CLIENT -> L("mode_member")
    }
}

enum class GameMode {
    LOCAL, P2P_HOST, P2P_CLIENT
}
