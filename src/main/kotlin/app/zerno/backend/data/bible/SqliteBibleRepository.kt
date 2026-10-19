package app.zerno.backend.data.bible

import app.zerno.backend.data.sql.query
import app.zerno.backend.domain.bible.BibleRepository
import app.zerno.backend.model.BibleBooksResponse
import app.zerno.backend.model.BookInfo
import app.zerno.backend.model.ChapterInfo
import app.zerno.backend.model.Verse
import app.zerno.backend.model.VersesPage
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class SqliteBibleRepository(
    private val db: Database,
    private val bibleId: String,
) : BibleRepository {

    override suspend fun getBooks(): BibleBooksResponse = db.query {
        val title = InfoTable.selectAll()
            .where { InfoTable.name eq "description" }
            .firstOrNull()?.get(InfoTable.value) ?: bibleId

        val chapterCount = VersesTable.chapter.countDistinct()
        val chaptersByBook = VersesTable
            .select(VersesTable.bookNumber, chapterCount)
            .groupBy(VersesTable.bookNumber)
            .associate { it[VersesTable.bookNumber] to it[chapterCount].toInt() }

        val books = BooksTable.selectAll()
            .orderBy(BooksTable.bookNumber)
            .mapIndexed { index, row ->
                val bookNumber = row[BooksTable.bookNumber]
                BookInfo(
                    id = bookNumber.toString(),
                    order = index + 1,
                    chapterCount = chaptersByBook[bookNumber] ?: 0,
                    displayName = row[BooksTable.longName],
                )
            }

        BibleBooksResponse(id = bibleId, displayName = title, books = books)
    }

    override suspend fun getChapters(bookNumber: Int): List<ChapterInfo> = db.query {
        val verseCount = VersesTable.verse.count()
        VersesTable
            .select(VersesTable.chapter, verseCount)
            .where { VersesTable.bookNumber eq bookNumber }
            .groupBy(VersesTable.chapter)
            .orderBy(VersesTable.chapter)
            .map {
                val chapter = it[VersesTable.chapter]
                ChapterInfo(
                    id = "$bookNumber:$chapter",
                    order = chapter,
                    verseCount = it[verseCount].toInt(),
                )
            }
    }

    override suspend fun getVerses(offset: Int, limit: Int): VersesPage = db.query {
        val total = VersesTable.selectAll().count().toInt()
        val verses = VersesTable.selectAll()
            .orderBy(
                VersesTable.bookNumber to SortOrder.ASC,
                VersesTable.chapter to SortOrder.ASC,
                VersesTable.verse to SortOrder.ASC,
            )
            .limit(limit)
            .offset(offset.toLong())
            .map {
                Verse(
                    book = it[VersesTable.bookNumber],
                    chapter = it[VersesTable.chapter],
                    order = it[VersesTable.verse],
                    text = it[VersesTable.text],
                )
            }

        VersesPage(offset = offset, limit = limit, total = total, verses = verses)
    }
}
