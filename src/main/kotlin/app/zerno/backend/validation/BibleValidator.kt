package app.zerno.backend.validation

import org.sqlite.SQLiteConfig
import java.io.File
import java.security.MessageDigest
import java.sql.Connection

/**
 * What [BibleValidator] found in one file. [errors] break schema v1 (`docs/bible-schema-v1.md`): the app could not
 * read the file as specified. [warnings] are allowed by v1 but worth a look: verse counts that differ from the
 * reference numbering (a translation that leaves out a disputed verse) and markup spans that do not balance.
 */
data class ValidationReport(
    val file: File,
    val sizeBytes: Long,
    val sha256: String,
    val errors: List<String>,
    val warnings: List<String>,
) {
    val passes: Boolean get() = errors.isEmpty()
}

/** Checks a Bible file against schema v1. The file is opened read-only and never changed. */
class BibleValidator(private val reference: (russianNumbering: Boolean) -> Versification = Versification::of) {

    fun validate(file: File): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val config = SQLiteConfig().apply { setReadOnly(true) }
        config.createConnection("jdbc:sqlite:${file.absolutePath}").use { db ->
            val tables = db.strings("SELECT name FROM sqlite_master WHERE type = 'table'").toSet()
            val missing = REQUIRED_TABLES - tables
            if (missing.isNotEmpty()) {
                errors += "missing tables: ${missing.sorted().joinToString()}"
            } else {
                val russianNumbering = checkInfo(db, errors)
                checkIndex(db, errors)
                val books = checkBooks(db, reference(russianNumbering ?: false), errors)
                checkVerses(db, books, reference(russianNumbering ?: false), errors, warnings)
                checkMarkup(db, errors, warnings)
            }
        }
        return ValidationReport(file, file.length(), sha256(file), errors, warnings)
    }

    /** Returns `russian_numbering`, or null when it is missing or not a boolean. */
    private fun checkInfo(db: Connection, errors: MutableList<String>): Boolean? {
        val info = db.rows("SELECT name, value FROM info") { it.getString(1) to it.getString(2) }.toMap()
        for (key in REQUIRED_INFO) {
            if (info[key].isNullOrBlank()) errors += "info: missing `$key`"
        }
        val language = info["language"]
        if (language != null && !LANGUAGE.matches(language)) errors += "info: `language` is not an ISO 639-1 code: $language"
        val numbering = info["russian_numbering"]
        return when (numbering) {
            "true" -> true
            "false" -> false
            null -> null
            else -> {
                errors += "info: `russian_numbering` must be true or false, got $numbering"
                null
            }
        }
    }

    private fun checkIndex(db: Connection, errors: MutableList<String>) {
        val unique = db.rows("PRAGMA index_list(verses)") { it.getString("name") to it.getInt("unique") }
            .filter { (_, isUnique) -> isUnique == 1 }
            .map { (name, _) -> db.strings("SELECT name FROM pragma_index_info('${name.replace("'", "''")}')") }
        if (unique.none { it == listOf("book_number", "chapter", "verse") }) {
            errors += "verses: no unique index on (book_number, chapter, verse)"
        }
    }

    /** Returns the book numbers present. */
    private fun checkBooks(db: Connection, reference: Versification, errors: MutableList<String>): Set<Int> {
        val rows = db.rows("SELECT book_number, short_name, long_name, book_color FROM books") {
            BookRow(it.getInt(1), it.getString(2), it.getString(3), it.getString(4))
        }
        val numbers = rows.map { it.number }
        if (numbers.size != numbers.toSet().size) errors += "books: a book_number appears twice"
        val expected = reference.books.keys
        val unknown = numbers.toSet() - expected
        val absent = expected - numbers.toSet()
        if (unknown.isNotEmpty()) errors += "books: not in the 66-book numbering: ${unknown.sorted().joinToString()}"
        if (absent.isNotEmpty()) errors += "books: missing ${absent.sorted().joinToString()}"
        for (book in rows) {
            if (book.shortName.isNullOrBlank() || book.longName.isNullOrBlank()) errors += "books: ${book.number} has no name"
            if (book.color == null || !COLOR.matches(book.color)) errors += "books: ${book.number} colour is not #rrggbb"
        }
        return numbers.toSet()
    }

    private fun checkVerses(
        db: Connection,
        books: Set<Int>,
        reference: Versification,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val empty = db.rows("SELECT count(*) FROM verses WHERE text IS NULL OR trim(text) = ''") { it.getInt(1) }.single()
        if (empty > 0) errors += "verses: $empty with no text"
        val orphans = db.rows("SELECT DISTINCT book_number FROM verses") { it.getInt(1) }.filter { it !in books }
        if (orphans.isNotEmpty()) errors += "verses: book_number not in books: ${orphans.sorted().joinToString()}"

        val chapters = db.rows(
            "SELECT book_number, chapter, count(*), min(verse), max(verse) FROM verses " +
                "GROUP BY book_number, chapter ORDER BY book_number, chapter",
        ) { Chapter(it.getInt(1), it.getInt(2), it.getInt(3), it.getInt(4), it.getInt(5)) }
        for ((book, list) in chapters.groupBy { it.book }) {
            val expected = reference.books[book] ?: continue
            val numbers = list.map { it.number }
            if (numbers != (1..numbers.size).toList()) {
                errors += "book $book: chapters are not numbered 1 to ${numbers.size}"
                continue
            }
            if (numbers.size != expected.size) {
                warnings += "book $book: ${numbers.size} chapters, the reference has ${expected.size}"
            }
            for (chapter in list) {
                if (chapter.min < 1) errors += "book $book chapter ${chapter.number}: verse ${chapter.min}"
                val gaps = chapter.max - chapter.count
                if (gaps > 0) warnings += "book $book chapter ${chapter.number}: $gaps verse number(s) skipped"
                val want = expected.getOrNull(chapter.number - 1)
                if (want != null && want != chapter.max) {
                    warnings += "book $book chapter ${chapter.number}: ${chapter.max} verses, the reference has $want"
                }
            }
        }
    }

    private fun checkMarkup(db: Connection, errors: MutableList<String>, warnings: MutableList<String>) {
        val unknown = sortedSetOf<String>()
        var unbalanced = 0
        db.rows("SELECT text FROM verses") { it.getString(1).orEmpty() }.forEach { text ->
            val open = ArrayDeque<String>()
            var balanced = true
            for (match in TAG.findAll(text)) {
                val (closing, name, selfClosing) = match.destructured
                when {
                    selfClosing.isNotEmpty() -> if (name !in SELF_CLOSING) unknown += "<$name/>"
                    name !in PAIRED -> unknown += "<$closing$name>"
                    closing.isEmpty() -> open.addLast(name)
                    open.lastOrNull() == name -> open.removeLast()
                    else -> balanced = false
                }
            }
            if (!balanced || open.isNotEmpty()) unbalanced++
        }
        if (unknown.isNotEmpty()) errors += "markup: tags not in v1: ${unknown.joinToString()}"
        if (unbalanced > 0) warnings += "markup: $unbalanced verse(s) with a span that does not close or open"
    }

    private data class BookRow(val number: Int, val shortName: String?, val longName: String?, val color: String?)

    private data class Chapter(val book: Int, val number: Int, val count: Int, val min: Int, val max: Int)

    companion object {
        val REQUIRED_TABLES = setOf("info", "books", "verses")
        val REQUIRED_INFO = listOf("language", "russian_numbering", "description")
        val PAIRED = setOf("S", "t", "J", "i", "em", "e", "n", "f")
        val SELF_CLOSING = setOf("pb", "br")
        private val TAG = Regex("<(/?)([A-Za-z]+)\\s*(/?)>")
        private val COLOR = Regex("#[0-9a-fA-F]{6}")
        private val LANGUAGE = Regex("[a-z]{2}")

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

private fun <T> Connection.rows(sql: String, read: (java.sql.ResultSet) -> T): List<T> =
    createStatement().use { statement ->
        statement.executeQuery(sql).use { result ->
            buildList { while (result.next()) add(read(result)) }
        }
    }

private fun Connection.strings(sql: String): List<String> = rows(sql) { it.getString(1) }
