package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.i18n.LocalizationManager
import jp.riverapp.blockpanic.i18n.Strings
import org.junit.Assert.*
import org.junit.Test

class LocalizationTest {

    // All supported language codes
    private val supportedCodes = LocalizationManager.supportedLanguages.map { it.first }

    // -- Strings.get() --

    @Test
    fun testEnglishBaseLanguage() {
        val text = Strings.get("play", language = "en")
        assertEquals("PLAY", text)
    }

    @Test
    fun testJapaneseTranslation() {
        val text = Strings.get("play", language = "ja")
        assertEquals("\u30D7\u30EC\u30A4", text) // プレイ
    }

    @Test
    fun testAllSupportedLanguagesHavePlayKey() {
        for (code in supportedCodes) {
            val text = Strings.get("play", language = code)
            assertFalse("$code should have 'play' translation", text.isEmpty())
            assertNotEquals("$code should not return raw key", "play", text)
        }
    }

    @Test
    fun testUnsupportedLanguageFallsBackToEnglish() {
        val text = Strings.get("play", language = "xyz")
        assertEquals("PLAY", text)
    }

    @Test
    fun testMissingKeyReturnsKeyItself() {
        val text = Strings.get("nonexistent_key_xyz", language = "en")
        assertEquals("nonexistent_key_xyz", text)
    }

    // -- All languages have core keys --

    @Test
    fun testAllLanguagesHaveCoreKeys() {
        val coreKeys = listOf(
            "play", "online", "game_over", "play_again", "settings",
            "how_to_play", "my_records", "back", "connecting",
            "host_disconnected", "language", "auto_device"
        )
        for (code in supportedCodes) {
            for (key in coreKeys) {
                val text = Strings.get(key, language = code)
                assertNotEquals("$code missing key: $key", key, text)
            }
        }
    }

    @Test
    fun testAllLanguagesHaveEffectKeys() {
        val keys = listOf("effect_big", "effect_mini", "effect_jump", "effect_points")
        for (code in supportedCodes) {
            for (key in keys) {
                val text = Strings.get(key, language = code)
                assertFalse("$code missing key: $key", text.isEmpty())
            }
        }
    }

    @Test
    fun testAllLanguagesHaveModeKeys() {
        val keys = listOf("mode_single", "mode_host", "mode_member")
        for (code in supportedCodes) {
            for (key in keys) {
                val text = Strings.get(key, language = code)
                assertNotEquals("$code missing key: $key", key, text)
            }
        }
    }

    @Test
    fun testAllLanguagesHaveHowToPlayKeys() {
        val keys = listOf("tip_move", "tip_jump", "tip_survive", "tip_coins", "tip_mystery", "tip_online")
        for (code in supportedCodes) {
            for (key in keys) {
                val text = Strings.get(key, language = code)
                assertNotEquals("$code missing key: $key", key, text)
            }
        }
    }

    // -- 15 languages registered --

    @Test
    fun testSupportedLanguagesCount() {
        assertEquals(15, LocalizationManager.supportedLanguages.size)
    }

    @Test
    fun testAllLanguagesHaveTranslationDictionary() {
        for (code in supportedCodes) {
            // Verify each language returns a valid translation for a known key
            val text = Strings.get("play", language = code)
            assertFalse("$code has no translation dictionary", text.isEmpty())
        }
    }

    // -- Language resolution --

    @Test
    fun testChineseSimplified() {
        val text = Strings.get("play", language = "zh-Hans")
        assertEquals("\u5F00\u59CB", text) // 开始
    }

    @Test
    fun testChineseTraditional() {
        val text = Strings.get("play", language = "zh-Hant")
        assertEquals("\u958B\u59CB", text) // 開始
    }
}
