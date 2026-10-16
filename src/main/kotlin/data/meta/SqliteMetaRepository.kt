package data.meta

import com.royaal.rbchurch.main.domain.meta.MetaRepository
import com.royaal.rbchurch.main.model.Bible
import com.royaal.rbchurch.main.model.Language
import domain.meta.model.BibleMeta
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SqliteMetaRepository(
    private val db: Database,
) : MetaRepository {

    override suspend fun getLanguages(): List<Language> =
        transaction(db) {
            MetaLanguagesTable.selectAll().map {
                Language(
                    id = it[MetaLanguagesTable.id],
                    name = it[MetaLanguagesTable.name],
                    localName = it[MetaLanguagesTable.localName],
                )
            }
        }

    override suspend fun getBiblesByLanguage(langId: String): List<Bible> =
        transaction(db) {
            MetaBiblesTable
                .selectAll()
                .where { MetaBiblesTable.languageId eq langId }
                .map { row ->
                    Bible(
                        id = row[MetaBiblesTable.id],
                        displayName = row[MetaBiblesTable.displayName],
                        bookCount = 66,
                        languageId = row[MetaBiblesTable.languageId]
                    )
                }
        }

    override suspend fun getBibleMeta(bibleId: String): BibleMeta =
        transaction(db) {
            MetaBiblesTable
                .selectAll()
                .where { MetaBiblesTable.id eq bibleId }
                .first()
                .let {
                    BibleMeta(
                        id = it[MetaBiblesTable.id],
                        displayName = it[MetaBiblesTable.displayName],
                        languageId = it[MetaBiblesTable.languageId],
                        dbType = BibleMeta.BibleDbType.SQLITE,
                        dbPathOrUrl = it[MetaBiblesTable.dbPath]
                    )
                }
        }
}
