package com.royaal.rbchurch.main.domain.bible

import domain.meta.model.BibleMeta
import com.royaal.rbchurch.main.model.BibleBooksResponse
import com.royaal.rbchurch.main.model.VersesPage
import model.ChapterInfo

interface BibleRepository {

    suspend fun getBooks(): BibleBooksResponse

    suspend fun getChapters(bookId: String): List<ChapterInfo>

    suspend fun getVerses(
        offset: Int,
        limit: Int,
    ): VersesPage

    interface Factory {
        fun create(meta: BibleMeta): BibleRepository
    }
}
