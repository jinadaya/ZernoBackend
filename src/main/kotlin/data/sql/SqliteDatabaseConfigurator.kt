package data.sql

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

object SqliteDatabaseConfigurator {

    fun configure(db: Database) {
        DriverManager.getConnection(db.url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode=WAL;")
                stmt.execute("PRAGMA synchronous=NORMAL;")
                stmt.execute("PRAGMA foreign_keys=ON;")
            }
        }
    }

    fun createIndexes(db: Database) {
        transaction(db) {
            exec(
                """
            CREATE INDEX IF NOT EXISTS idx_verses_book_chapter_verse
            ON verses(book_number, chapter, verse)
        """.trimIndent()
            )

            exec(
                """
            CREATE INDEX IF NOT EXISTS idx_books_sorting
            ON books(book_number)
        """.trimIndent()
            )
        }
    }

}