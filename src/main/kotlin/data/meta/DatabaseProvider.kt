package data.meta

import org.jetbrains.exposed.sql.Database

object DatabaseProvider {

    fun connectSqlite(path: String): Database =
        Database.connect(
            url = "jdbc:sqlite:$path",
            driver = "org.sqlite.JDBC"
        )
}
