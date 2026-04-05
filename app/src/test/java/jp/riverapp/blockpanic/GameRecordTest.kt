package jp.riverapp.blockpanic

import com.google.gson.Gson
import jp.riverapp.blockpanic.model.GameRecord
import org.junit.Assert.*
import org.junit.Test

/**
 * GameRecord model and serialization tests.
 * Note: SharedPreferences (GameRecordStore) cannot be tested in unit tests.
 * We test the model, Gson serialization, and formattedDate only.
 */
class GameRecordTest {

    private val gson = Gson()

    // -- Codable / Serialization --

    @Test
    fun testRecordRoundTrip() {
        val original = GameRecord(
            survivalTime = 42,
            score = 4200,
            mode = "Online(Host)",
            playerName = "Alice"
        )
        val json = gson.toJson(original)
        val decoded = gson.fromJson(json, GameRecord::class.java)
        assertEquals(original.id, decoded.id)
        assertEquals(42, decoded.survivalTime)
        assertEquals(4200, decoded.score)
        assertEquals("Online(Host)", decoded.mode)
        assertEquals("Alice", decoded.playerName)
    }

    @Test
    fun testRecordDefaultValues() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "Test")
        assertFalse(record.id.isEmpty())
        assertTrue(record.date > 0)
    }

    @Test
    fun testRecordAllModes() {
        val modes = listOf("Single", "Online(Host)", "Online(Member)")
        for (mode in modes) {
            val record = GameRecord(survivalTime = 10, score = 100, mode = mode, playerName = "P")
            val json = gson.toJson(record)
            val decoded = gson.fromJson(json, GameRecord::class.java)
            assertEquals(mode, decoded.mode)
        }
    }

    // -- Date format --

    @Test
    fun testFormattedDate() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "Test")
        val formatted = record.formattedDate
        assertFalse(formatted.isEmpty())
        assertTrue("Date should contain '/' separator", formatted.contains("/"))
        assertTrue("Date should contain ':' separator", formatted.contains(":"))
    }

    // -- Multiple records serialization --

    @Test
    fun testMultipleRecordsRoundTrip() {
        val records = (1..5).map { i ->
            GameRecord(survivalTime = i * 10, score = i * 100, mode = "Single", playerName = "P$i")
        }
        val json = gson.toJson(records)
        val decoded: List<GameRecord> = gson.fromJson(
            json, Array<GameRecord>::class.java
        ).toList()
        assertEquals(5, decoded.size)
        assertEquals("P1", decoded[0].playerName)
        assertEquals("P5", decoded[4].playerName)
    }
}
