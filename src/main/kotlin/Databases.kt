import com.royaal.rbchurch.main.domain.meta.MetaRepository
import data.bible.DefaultBibleRepositoryFactory
import data.meta.DatabaseProvider
import data.meta.SqliteMetaRepository
import io.ktor.server.application.*
import mainApp.AppContext
import java.io.File

lateinit var appContext: AppContext

fun Application.configureDatabases() {

    val metaDbPath = environment.config
        .property("bible.metaDbPath")
        .getString()
    val dbFile = File(metaDbPath)

    if (dbFile.exists()) {
        val metaDb = DatabaseProvider.connectSqlite(dbFile.absolutePath)
        val metaRepository: MetaRepository = SqliteMetaRepository(metaDb)
        val bibleRepositoryFactory = DefaultBibleRepositoryFactory()

        appContext = AppContext(
            metaRepository = metaRepository,
            bibleRepositoryFactory = bibleRepositoryFactory
        )

        log.info("AppContext initialized")
    } else {
        throw IllegalStateException("No meta database with path: $metaDbPath exists")
    }
}