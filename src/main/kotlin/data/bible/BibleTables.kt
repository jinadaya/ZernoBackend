package data.bible

import org.jetbrains.exposed.sql.Table

object InfoTable : Table("info") {
    val name = text("name")
    val value = text("value")
}

object BooksTable : Table("books") {
    val bookNumber = integer("book_number")
    val shortName = text("short_name")
    val longName = text("long_name")
}

object VersesTable : Table("verses") {
    val bookNumber = integer("book_number")
    val chapter = integer("chapter")
    val verse = integer("verse")
    val text = text("text")
}