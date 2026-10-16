package data.bible.search

import org.jetbrains.exposed.sql.Table

object VersesFtsTable : Table("verses_fts") {
    val bookNumber = integer("book_number")
    val chapter = integer("chapter")
    val verse = integer("verse")
    val text = text("text")
}
