package app.zerno.backend.plugins

import app.zerno.backend.BibleConfig
import app.zerno.backend.content.ContentCatalog
import app.zerno.backend.data.bible.BibleRepositories
import app.zerno.backend.data.bible.SqliteBibleRepositoryFactory
import app.zerno.backend.data.meta.SqliteMetaRepository
import app.zerno.backend.data.sql.SqliteDatabases
import app.zerno.backend.domain.bible.BibleRepository
import app.zerno.backend.domain.meta.MetaRepository
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.KoinIsolated
import org.koin.logger.slf4jLogger

fun appModule(config: BibleConfig) = module {
    single { config }
    single<MetaRepository>(createdAtStart = true) {
        check(config.metaDbFile.exists()) { "No meta database at ${config.metaDbFile.absolutePath}" }
        SqliteMetaRepository(SqliteDatabases.openReadOnly(config.metaDbFile))
    }
    single<BibleRepository.Factory> { SqliteBibleRepositoryFactory(config.biblesDir) }
    single { BibleRepositories(get(), get()) }
    single(createdAtStart = true) { ContentCatalog(config.contentDir) }
}

fun Application.configureDependencies(config: BibleConfig) {
    install(KoinIsolated) {
        slf4jLogger()
        modules(appModule(config))
        createEagerInstances()
    }
}
