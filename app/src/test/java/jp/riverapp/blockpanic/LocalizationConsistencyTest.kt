package jp.riverapp.blockpanic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ローカライズリソースの整合性チェック.
 *
 * - `values-id` (Android 7.0+) と `values-in` (Android legacy code) は同内容を保つ必要あり.
 *   ロケール id → Android は内部的に in にマップするが、旧端末互換のため両方配置している.
 *   編集時の同期漏れを防ぐためのガード.
 * - アプリ名は 15 言語すべてで定義されている必要あり.
 */
class LocalizationConsistencyTest {

    private val resDir: File by lazy {
        // unit test の作業ディレクトリは app/ なので直接参照
        File("src/main/res").absoluteFile.also {
            assertTrue("res dir not found: $it", it.isDirectory)
        }
    }

    @Test
    fun `values-id and values-in stay in sync`() {
        val id = File(resDir, "values-id/strings.xml").readText()
        val `in` = File(resDir, "values-in/strings.xml").readText()
        assertEquals(
            "values-id と values-in の内容が不一致. 翻訳更新時は両方を同じ内容で更新すること.",
            id, `in`
        )
    }

    @Test
    fun `app_name is defined in all 15 supported locales`() {
        val requiredDirs = listOf(
            "values",             // default (en)
            "values-ja",
            "values-b+zh+Hans",
            "values-b+zh+Hant",
            "values-ko",
            "values-fr",
            "values-de",
            "values-es",
            "values-pt",
            "values-it",
            "values-ru",
            "values-th",
            "values-vi",
            "values-in",
            "values-id",
            "values-hi",
        )
        for (dir in requiredDirs) {
            val f = File(resDir, "$dir/strings.xml")
            assertTrue("Missing strings.xml: $dir", f.isFile)
            val content = f.readText()
            assertTrue(
                "$dir/strings.xml does not contain app_name",
                content.contains("name=\"app_name\"")
            )
        }
    }
}
