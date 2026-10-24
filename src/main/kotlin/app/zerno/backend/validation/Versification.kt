package app.zerno.backend.validation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Verses per chapter for each `book_number` in one numbering tradition. The two shipped references,
 * `versification/kjv.json` and `versification/synodal.json`, were taken from the bundled KJV and RST files.
 */
@Serializable
data class Versification(val books: Map<Int, List<Int>>) {
    companion object {
        val Kjv: Versification by lazy { load("kjv") }
        val Synodal: Versification by lazy { load("synodal") }

        /** The reference for a file's `russian_numbering` value. */
        fun of(russianNumbering: Boolean): Versification = if (russianNumbering) Synodal else Kjv

        private fun load(name: String): Versification {
            val text = checkNotNull(Versification::class.java.getResource("/versification/$name.json")) {
                "No versification/$name.json on the classpath"
            }.readText()
            return Json { ignoreUnknownKeys = true }.decodeFromString(text)
        }
    }
}
