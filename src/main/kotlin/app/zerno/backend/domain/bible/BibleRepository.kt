package app.zerno.backend.domain.bible

import app.zerno.backend.domain.meta.model.BibleMeta
import app.zerno.backend.model.BibleBooksResponse
import app.zerno.backend.model.ChapterInfo
import app.zerno.backend.model.VersesPage

interface BibleRepository {

    suspend fun getBooks(): BibleBooksResponse

    /** Chapters of a book by its MyBible `book_number` (10 = Genesis); empty for an unknown book. */
    suspend fun getChapters(bookNumber: Int): List<ChapterInfo>

    suspend fun getVerses(offset: Int, limit: Int): VersesPage

    fun interface Factory {
        fun create(meta: BibleMeta): BibleRepository
    }
}
