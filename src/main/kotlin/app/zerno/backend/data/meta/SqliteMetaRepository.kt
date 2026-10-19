package app.zerno.backend.data.meta

import app.zerno.backend.data.sql.query
import app.zerno.backend.domain.meta.MetaRepository
import app.zerno.backend.domain.meta.model.BibleMeta
import app.zerno.backend.model.Bible
import app.zerno.backend.model.Language
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll

class SqliteMetaRepository(
    private val db: Database,
) : MetaRepository {

    override suspend fun getLanguages(): List<Language> = db.query {
        MetaLanguagesTable.selectAll().orderBy(MetaLanguagesTable.id).map { it.toLanguage() }
    }

    override suspend fun getLanguage(langId: String): Language? = db.query {
        MetaLanguagesTable.selectAll()
            .where { MetaLanguagesTable.id eq langId }
            .firstOrNull()
            ?.toLanguage()
    }

    override suspend fun getBiblesByLanguage(langId: String): List<Bible> = db.query {
        MetaBiblesTable.selectAll()
            .where { MetaBiblesTable.languageId eq langId }
            .orderBy(MetaBiblesTable.id)
            .map { row ->
                Bible(
                    id = row[MetaBiblesTable.id],
                    translationCode = row[MetaBiblesTable.translationCode],
                    displayName = row[MetaBiblesTable.displayName],
                    bookCount = 66,
                    languageId = row[MetaBiblesTable.languageId],
                    licence = row[MetaBiblesTable.licence],
                    attribution = row[MetaBiblesTable.attribution],
                )
            }
    }

    override suspend fun getBibleMeta(bibleId: String): BibleMeta? = db.query {
        MetaBiblesTable.selectAll()
            .where { MetaBiblesTable.id eq bibleId }
            .firstOrNull()
            ?.let {
                BibleMeta(
                    id = it[MetaBiblesTable.id],
                    displayName = it[MetaBiblesTable.displayName],
                    languageId = it[MetaBiblesTable.languageId],
                    dbType = BibleMeta.BibleDbType.SQLITE,
                    dbPath = it[MetaBiblesTable.dbPath],
                )
            }
    }

    private fun ResultRow.toLanguage() = Language(
        id = this[MetaLanguagesTable.id],
        name = this[MetaLanguagesTable.name],
        localName = this[MetaLanguagesTable.localName],
    )
}
