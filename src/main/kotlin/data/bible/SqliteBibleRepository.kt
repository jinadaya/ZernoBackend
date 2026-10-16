package data.bible

import com.royaal.rbchurch.main.domain.bible.BibleRepository
import com.royaal.rbchurch.main.model.BibleBooksResponse
import com.royaal.rbchurch.main.model.BookInfo
import com.royaal.rbchurch.main.model.Verse
import com.royaal.rbchurch.main.model.VersesPage
import model.ChapterInfo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SqliteBibleRepository(
    private val db: Database,
    private val bibleId: String,
) : BibleRepository {

    override suspend fun getBooks(): BibleBooksResponse = transaction(db) {

        val title = InfoTable.selectAll()
            .where(InfoTable.name eq "description")
            .firstOrNull()?.get(InfoTable.value) ?: bibleId

        val books = BooksTable.selectAll().orderBy(BooksTable.bookNumber).map {
            val bookNumber = it[BooksTable.bookNumber]

            BookInfo(
                id = (bookNumber / 10).toString(),
                order = it[BooksTable.bookNumber] / 10,
                chapterCount = countChapters(bookNumber),
                displayName = it[BooksTable.longName],
            )
        }

        BibleBooksResponse(
            id = bibleId,
            displayName = title,
            books = books,
        )
    }

    override suspend fun getChapters(bookId: String): List<ChapterInfo> = transaction(db) {
        val bookNumId = bookId.toInt()

        val chapters = VersesTable
            .selectAll()
            .where(VersesTable.bookNumber eq bookNumId)
            .distinctBy { row -> row[VersesTable.chapter] }
            .map { row ->
                val chapter = row[VersesTable.chapter]

                val count = VersesTable
                    .selectAll()
                    .where((VersesTable.bookNumber eq bookNumId) and (VersesTable.chapter eq chapter))
                    .count()

                ChapterInfo(
                    id = "$bookNumId:$chapter",
                    order = chapter,
                    verseCount = count.toInt(),
                )
            }
        return@transaction chapters
    }

    override suspend fun getVerses(
        offset: Int,
        limit: Int,
    ): VersesPage = transaction(db) {
        val verses = VersesTable
            .selectAll()
            .limit(limit)
            .offset(offset.toLong())
            .map {
                Verse(
                    order = it[VersesTable.verse],
                    text = it[VersesTable.text],
                    book = it[VersesTable.bookNumber],
                    chapter = it[VersesTable.chapter],
                )
            }

        VersesPage(
            offset = offset,
            limit = limit,
            total = verses.size,
            verses = verses
        )
    }

    private fun countChapters(bookNumber: Int): Int = VersesTable
        .selectAll()
        .where(VersesTable.bookNumber eq bookNumber)
        .distinctBy { row -> row[VersesTable.chapter] }
        .count()
}
