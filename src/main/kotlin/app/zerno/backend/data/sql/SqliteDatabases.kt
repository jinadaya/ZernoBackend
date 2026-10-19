package app.zerno.backend.data.sql

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteConfig
import java.io.File

object SqliteDatabases {

    /**
     * Bible files are served as they are (and later downloaded with a checksum),
     * so the server opens them read-only and never adds indexes or WAL files to them.
     */
    fun openReadOnly(file: File): Database {
        val url = "jdbc:sqlite:${file.absolutePath}"
        val config = SQLiteConfig().apply { setReadOnly(true) }
        return Database.connect(
            getNewConnection = { config.createConnection(url) },
            // sqlite-jdbc only accepts the read-only flag at connect time, so Exposed must ask for the same.
            databaseConfig = DatabaseConfig { defaultReadOnly = true },
        )
    }
}

suspend fun <T> Database.query(block: Transaction.() -> T): T =
    withContext(Dispatchers.IO) { transaction(this@query) { block() } }
