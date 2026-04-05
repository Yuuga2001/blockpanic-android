package jp.riverapp.blockpanic.i18n

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * App-wide language management.
 * Port from iOS LocalizationManager.swift
 */
object LocalizationManager {

    const val AUTO_CODE = "auto"

    val supportedLanguages: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ja" to "日本語",
        "zh-Hans" to "简体中文",
        "zh-Hant" to "繁體中文",
        "ko" to "한국어",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "pt" to "Português",
        "it" to "Italiano",
        "ru" to "Русский",
        "th" to "ไทย",
        "vi" to "Tiếng Việt",
        "id" to "Indonesia",
        "hi" to "हिन्दी",
    )

    private val supportedCodes = supportedLanguages.map { it.first }.toSet()

    @Volatile
    var currentLanguage: String = "en"
        private set

    private var prefs: SharedPreferences? = null

    /** Must be called once at app start (e.g., in Application.onCreate or MainActivity.onCreate) */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("blockpanic_prefs", Context.MODE_PRIVATE)
        val saved = prefs?.getString("appLanguage", null)
        currentLanguage = if (saved != null && saved != AUTO_CODE && saved in supportedCodes) {
            saved
        } else {
            resolveLanguage(Locale.getDefault().toLanguageTag())
        }
    }

    /** The stored setting value ("auto" or language code) */
    val selectedCode: String
        get() = prefs?.getString("appLanguage", AUTO_CODE) ?: AUTO_CODE

    fun setLanguage(code: String) {
        prefs?.edit()?.putString("appLanguage", code)?.apply()
        currentLanguage = if (code == AUTO_CODE) {
            resolveLanguage(Locale.getDefault().toLanguageTag())
        } else {
            code
        }
    }

    private fun resolveLanguage(preferredLanguage: String): String {
        if (preferredLanguage in supportedCodes) return preferredLanguage

        val prefix = preferredLanguage.take(2)
        if (prefix == "zh") {
            return if (preferredLanguage.contains("Hant") || preferredLanguage.contains("TW") || preferredLanguage.contains("HK")) {
                "zh-Hant"
            } else {
                "zh-Hans"
            }
        }
        if (prefix in supportedCodes) return prefix
        return "en"
    }
}

/** Global localization helper */
fun L(key: String): String {
    return Strings.get(key, LocalizationManager.currentLanguage)
}
