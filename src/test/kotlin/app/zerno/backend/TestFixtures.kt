package app.zerno.backend

import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager

/**
 * A tiny meta database and one tiny Bible in MyBible layout, so route tests
 * don't depend on the real (large, partly licensed) files.
 */
object TestFixtures {

    val dir: File by lazy { Files.createTempDirectory("zerno-test").toFile().also(::create) }

    fun config() = MapApplicationConfig(
        "bible.metaDbPath" to File(dir, "meta.SQLite3").absolutePath,
        "bible.biblesDir" to File(dir, "bibles").absolutePath,
        "content.dir" to File(dir, "content").absolutePath,
    )

    private fun create(root: File) {
        writeContent(File(root, "content"))
        sqlite(File(root, "meta.SQLite3"), File("databases/meta/meta.sql").readText().substringBefore("INSERT"))
        sqlite(
            File(root, "meta.SQLite3"),
            """
            INSERT INTO languages VALUES ('en', 'English', 'English'), ('xx', 'Empty', 'Empty');
            INSERT INTO bibles VALUES
                ('tst', 'TST', 'Test Bible', 'en', 'public-domain', 'Test attribution', 'sqlite', 'tst.SQLite3'),
                ('gone', 'GONE', 'Missing file', 'en', 'public-domain', 'Gone', 'sqlite', 'gone.SQLite3');
            """,
        )
        sqlite(
            File(root, "bibles/en/tst.SQLite3").also { it.parentFile.mkdirs() },
            """
            CREATE TABLE info (name TEXT, value TEXT);
            CREATE TABLE books (book_color TEXT, book_number INTEGER, short_name TEXT, long_name TEXT);
            CREATE TABLE verses (book_number NUMERIC, chapter NUMERIC, verse NUMERIC, text TEXT);
            INSERT INTO info VALUES ('description', 'Test Bible');
            INSERT INTO books VALUES ('#ccccff', 20, 'Exo', 'Exodus'), ('#ccccff', 10, 'Gen', 'Genesis');
            INSERT INTO verses VALUES
                (20, 1, 1, 'Exodus 1:1'), (20, 1, 2, 'Exodus 1:2'),
                (10, 2, 1, 'Genesis 2:1'),
                (10, 1, 3, 'Genesis 1:3'), (10, 1, 1, 'Genesis 1:1'), (10, 1, 2, 'Genesis 1:2');
            """,
        )
    }

    /** Two one-day plans listed second-first, and two achievements. */
    fun writeContent(dir: File) {
        File(dir, "plans").mkdirs()
        File(dir, "plans/index.json").writeText("""["two", "one"]""")
        for (id in listOf("one", "two")) {
            File(dir, "plans/$id.json").writeText(
                """{"id": "$id", "version": 1, "days": [{"day": 1, "passages": ["John 1"], "minutes": 5}]}""",
            )
        }
        File(dir, "achievements.json").writeText(
            """{"version": 1, "achievements": [{"code": "first"}, {"code": "second"}]}""",
        )
    }

    fun sqlite(file: File, script: String) {
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                sqlStatements(script).forEach(stmt::executeUpdate)
            }
        }
    }

    fun sqlStatements(script: String): List<String> = script.lines()
        .filterNot { it.trimStart().startsWith("--") }
        .joinToString("\n")
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

fun fixtureApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    environment { config = TestFixtures.config() }
    application { module() }
    block()
}
