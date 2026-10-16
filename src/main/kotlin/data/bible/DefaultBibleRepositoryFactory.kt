package data.bible

import com.royaal.rbchurch.main.domain.bible.BibleRepository
import domain.meta.model.BibleMeta
import data.sql.SqliteDatabaseConfigurator
import org.jetbrains.exposed.sql.Database
import java.io.File

class DefaultBibleRepositoryFactory : BibleRepository.Factory {

    override fun create(meta: BibleMeta): BibleRepository {

        val pathToBible = DEFAULT_DB_PACKAGE_NAME + meta.languageId + "/" + meta.dbPathOrUrl
        val dbFile = File(pathToBible)
        if (dbFile.exists()) {
            println("$dbFile exists")
            val db = Database.connect(
                url = "jdbc:sqlite:${dbFile.absolutePath}",
                driver = "org.sqlite.JDBC"
            )

            SqliteDatabaseConfigurator.configure(db)
            SqliteDatabaseConfigurator.createIndexes(db)

            return SqliteBibleRepository(db, meta.id)
        } else {
            throw IllegalStateException("No Database found at path: $pathToBible")
        }
    }

    companion object {
        private const val DEFAULT_DB_PACKAGE_NAME = "databases/bibles/"
    }
}