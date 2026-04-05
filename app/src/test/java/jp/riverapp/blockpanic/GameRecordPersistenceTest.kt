package jp.riverapp.blockpanic

import com.google.gson.Gson
import jp.riverapp.blockpanic.model.GameRecord
import org.junit.Assert.*
import org.junit.Test

/**
 * Extended GameRecord model tests.
 * GameRecordStore requires SharedPreferences (Android context) so only
 * the model, serialization, and formatting are testable in unit tests.
 */
class GameRecordPersistenceTest {

    private val gson = Gson()

    // =========================================================================
    // formattedDate contains "/" and ":" separators
    // =========================================================================

    @Test
    fun `formattedDate contains slash separator`() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "Test")
        assertTrue("formattedDate should contain '/'", record.formattedDate.contains("/"))
    }

    @Test
    fun `formattedDate contains colon separator`() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "Test")
        assertTrue("formattedDate should contain ':'", record.formattedDate.contains(":"))
    }

    @Test
    fun `formattedDate format is yyyy_MM_dd HH_mm`() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "Test")
        val formatted = record.formattedDate
        // Should match pattern like "2026/04/05 12:34"
        val parts = formatted.split(" ")
        assertEquals("Should have date and time parts", 2, parts.size)
        val dateParts = parts[0].split("/")
        assertEquals("Date should have 3 parts (yyyy/MM/dd)", 3, dateParts.size)
        assertEquals("Year should be 4 digits", 4, dateParts[0].length)
        val timeParts = parts[1].split(":")
        assertEquals("Time should have 2 parts (HH:mm)", 2, timeParts.size)
    }

    // =========================================================================
    // Multiple records can be serialized/deserialized as array
    // =========================================================================

    @Test
    fun `multiple records serialize as JSON array`() {
        val records = listOf(
            GameRecord(survivalTime = 10, score = 1000, mode = "Single", playerName = "A"),
            GameRecord(survivalTime = 20, score = 2000, mode = "Online(Host)", playerName = "B"),
            GameRecord(survivalTime = 30, score = 3000, mode = "Online(Member)", playerName = "C")
        )
        val json = gson.toJson(records)
        assertTrue("JSON should start with [", json.startsWith("["))
        assertTrue("JSON should end with ]", json.endsWith("]"))

        val decoded: List<GameRecord> = gson.fromJson(json, Array<GameRecord>::class.java).toList()
        assertEquals(3, decoded.size)
    }

    @Test
    fun `deserialized array preserves order`() {
        val records = (1..10).map { i ->
            GameRecord(survivalTime = i * 10, score = i * 100, mode = "Single", playerName = "P$i")
        }
        val json = gson.toJson(records)
        val decoded: List<GameRecord> = gson.fromJson(json, Array<GameRecord>::class.java).toList()
        for (i in 0 until 10) {
            assertEquals("P${i + 1}", decoded[i].playerName)
            assertEquals((i + 1) * 100, decoded[i].score)
        }
    }

    @Test
    fun `empty array serializes and deserializes`() {
        val records = emptyList<GameRecord>()
        val json = gson.toJson(records)
        assertEquals("[]", json)
        val decoded: List<GameRecord> = gson.fromJson(json, Array<GameRecord>::class.java).toList()
        assertTrue(decoded.isEmpty())
    }

    // =========================================================================
    // Mode strings for all 3 modes are non-empty
    // =========================================================================

    @Test
    fun `Single mode string is non-empty`() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "Test")
        assertTrue(record.mode.isNotEmpty())
        assertEquals("Single", record.mode)
    }

    @Test
    fun `Online Host mode string is non-empty`() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Online(Host)", playerName = "Test")
        assertTrue(record.mode.isNotEmpty())
        assertEquals("Online(Host)", record.mode)
    }

    @Test
    fun `Online Member mode string is non-empty`() {
        val record = GameRecord(survivalTime = 10, score = 100, mode = "Online(Member)", playerName = "Test")
        assertTrue(record.mode.isNotEmpty())
        assertEquals("Online(Member)", record.mode)
    }

    // =========================================================================
    // Score unit display
    // =========================================================================

    @Test
    fun `score is an integer suitable for pt display`() {
        val record = GameRecord(survivalTime = 42, score = 4200, mode = "Single", playerName = "Alice")
        // Score is displayed as "${score}pt" in the UI
        val displayText = "${record.score}pt"
        assertEquals("4200pt", displayText)
    }

    @Test
    fun `zero score displays correctly`() {
        val record = GameRecord(survivalTime = 0, score = 0, mode = "Single", playerName = "Test")
        assertEquals("0pt", "${record.score}pt")
    }

    // =========================================================================
    // Records sorted by score
    // =========================================================================

    @Test
    fun `records can be sorted by score descending`() {
        val records = listOf(
            GameRecord(survivalTime = 10, score = 500, mode = "Single", playerName = "C"),
            GameRecord(survivalTime = 20, score = 3000, mode = "Single", playerName = "A"),
            GameRecord(survivalTime = 15, score = 1200, mode = "Single", playerName = "B")
        )
        val sorted = records.sortedByDescending { it.score }
        assertEquals(3000, sorted[0].score)
        assertEquals(1200, sorted[1].score)
        assertEquals(500, sorted[2].score)
    }

    @Test
    fun `records with same score maintain stable sort`() {
        val records = listOf(
            GameRecord(survivalTime = 10, score = 1000, mode = "Single", playerName = "First"),
            GameRecord(survivalTime = 20, score = 1000, mode = "Single", playerName = "Second")
        )
        val sorted = records.sortedByDescending { it.score }
        // Stable sort: same score preserves original order
        assertEquals("First", sorted[0].playerName)
        assertEquals("Second", sorted[1].playerName)
    }

    @Test
    fun `record id is unique UUID`() {
        val r1 = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "A")
        val r2 = GameRecord(survivalTime = 10, score = 100, mode = "Single", playerName = "A")
        assertNotEquals("Each record should have unique id", r1.id, r2.id)
    }
}
