package app.zerno.backend.domain.meta

import app.zerno.backend.domain.meta.model.BibleMeta
import app.zerno.backend.model.Bible
import app.zerno.backend.model.Language

interface MetaRepository {

    suspend fun getLanguages(): List<Language>

    suspend fun getLanguage(langId: String): Language?

    suspend fun getBiblesByLanguage(langId: String): List<Bible>

    /** Where a Bible's database lives, or null for an unknown Bible. */
    suspend fun getBibleMeta(bibleId: String): BibleMeta?

    /** Every Bible in the meta database, by id. */
    suspend fun getAllBibleMeta(): List<BibleMeta>
}
