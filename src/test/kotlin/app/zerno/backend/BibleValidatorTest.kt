package app.zerno.backend

import app.zerno.backend.validation.BibleValidator
import app.zerno.backend.validation.Versification
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleValidatorTest {
    /** Two books: book 10 with chapters of 2 and 1 verses, book 20 with one chapter of 2 verses. */
    private val reference = Versification(mapOf(10 to listOf(2, 1), 20 to listOf(2)))
    private val validator = BibleValidator { reference }

    private fun bible(
        info: String = "('language', 'en'), ('russian_numbering', 'false'), ('description', 'Test')",
        books: String = "('#ccccff', 10, 'Gen', 'Genesis'), ('#ccccff', 20, 'Exo', 'Exodus')",
        verses: String = "(10, 1, 1, 'In the <i>beginning</i>'), (10, 1, 2, 'a<S>1</S>'), (10, 2, 1, 'b<pb/>'), " +
            "(20, 1, 1, 'c'), (20, 1, 2, 'd')",
        index: Boolean = true,
    ): File {
        val file = File(Files.createTempDirectory("zerno-validate").toFile(), "tst.SQLite3")
        TestFixtures.sqlite(
            file,
            """
            CREATE TABLE info (name TEXT, value TEXT);
            CREATE TABLE books (book_color TEXT, book_number INTEGER, short_name TEXT, long_name TEXT);
            CREATE TABLE verses (book_number NUMERIC, chapter NUMERIC, verse NUMERIC, text TEXT);
            ${if (index) "CREATE UNIQUE INDEX verses_index ON verses (book_number, chapter, verse);" else ""}
            INSERT INTO info VALUES $info;
            INSERT INTO books VALUES $books;
            INSERT INTO verses VALUES $verses;
            """,
        )
        return file
    }

    @Test
    fun `a file that follows v1 passes with its checksum`() {
        val file = bible()
        val report = validator.validate(file)
        assertEquals(emptyList(), report.errors)
        assertEquals(emptyList(), report.warnings)
        assertEquals(BibleValidator.sha256(file), report.sha256)
        assertEquals(64, report.sha256.length)
        assertEquals(file.length(), report.sizeBytes)
    }

    @Test
    fun `missing info keys and a bad numbering flag are errors`() {
        val report = validator.validate(bible(info = "('language', 'english'), ('russian_numbering', 'yes')"))
        assertTrue(report.errors.any { "`description`" in it }, report.errors.toString())
        assertTrue(report.errors.any { "ISO 639-1" in it }, report.errors.toString())
        assertTrue(report.errors.any { "`russian_numbering` must be" in it }, report.errors.toString())
    }

    @Test
    fun `a missing book and an unknown book are errors`() {
        val report = validator.validate(
            bible(books = "('#ccccff', 10, 'Gen', 'Genesis'), ('#ccccff', 15, 'X', 'Extra')"),
        )
        assertTrue(report.errors.any { it == "books: missing 20" }, report.errors.toString())
        assertTrue(report.errors.any { "not in the 66-book numbering: 15" in it }, report.errors.toString())
    }

    @Test
    fun `no unique verse index is an error`() {
        val report = validator.validate(bible(index = false))
        assertTrue(report.errors.any { "unique index" in it }, report.errors.toString())
    }

    @Test
    fun `an unknown tag is an error and an unclosed span a warning`() {
        val report = validator.validate(
            bible(
                verses = "(10, 1, 1, '<b>bold</b>'), (10, 1, 2, '<J>open'), (10, 2, 1, 'b'), " +
                    "(20, 1, 1, 'c'), (20, 1, 2, 'd')",
            ),
        )
        assertEquals(listOf("markup: tags not in v1: </b>, <b>"), report.errors)
        assertEquals(listOf("markup: 1 verse(s) with a span that does not close or open"), report.warnings)
    }

    @Test
    fun `a skipped verse and a short chapter are warnings, an empty verse an error`() {
        val report = validator.validate(
            bible(verses = "(10, 1, 2, 'a'), (10, 2, 1, ''), (20, 1, 1, 'c')"),
        )
        assertEquals(listOf("verses: 1 with no text"), report.errors)
        assertEquals(
            listOf(
                "book 10 chapter 1: 1 verse number(s) skipped",
                "book 20 chapter 1: 1 verses, the reference has 2",
            ),
            report.warnings,
        )
    }

    @Test
    fun `the bundled Bibles pass`() {
        val validator = BibleValidator()
        for (path in listOf("en/kjv.SQLite3", "en/asv.SQLite3", "ru/rst.SQLite3")) {
            val report = validator.validate(File("databases/bibles/$path"))
            assertEquals(emptyList(), report.errors, path)
        }
    }

    @Test
    fun `the references cover 66 books`() {
        assertEquals(66, Versification.Kjv.books.size)
        assertEquals(31102, Versification.Kjv.books.values.sumOf { it.sum() })
        assertEquals(66, Versification.Synodal.books.size)
    }
}
