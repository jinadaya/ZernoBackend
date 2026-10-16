package com.royaal.rbchurch.main.domain.meta

import com.royaal.rbchurch.main.model.Bible
import com.royaal.rbchurch.main.model.Language
import domain.meta.model.BibleMeta

interface MetaRepository {

    /** getLangs */
    suspend fun getLanguages(): List<Language>

    /** getBibles */
    suspend fun getBiblesByLanguage(langId: String): List<Bible>

    /**
     * Возвращает описание Библии + информацию,
     * как подключиться к её БД
     */
    suspend fun getBibleMeta(bibleId: String): BibleMeta
}