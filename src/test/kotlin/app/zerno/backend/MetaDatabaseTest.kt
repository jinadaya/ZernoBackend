package app.zerno.backend

import app.zerno.backend.data.meta.SqliteMetaRepository
import app.zerno.backend.data.sql.SqliteDatabases
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks the checked-in meta database, which the server reads in production. */
class MetaDatabaseTest {

    private val metaDb = File("databases/meta/meta.SQLite3")
    private val metaSql = File("databases/meta/meta.sql")

    @Test
    fun `meta database matches meta sql`() {
        val fromSql = File(Files.createTempDirectory("meta").toFile(), "meta.SQLite3")
        TestFixtures.sqlite(fromSql, metaSql.readText())
        for (table in listOf("languages", "bibles")) {
            assertEquals(dump(fromSql, table), dump(metaDb, table), "$table differs; rebuild meta.SQLite3 from meta.sql")
        }
    }

    @Test
    fun `every bible has a code, licence and attribution`() = runBlocking {
        val repo = SqliteMetaRepository(SqliteDatabases.openReadOnly(metaDb))
        val bibles = repo.getLanguages().flatMap { repo.getBiblesByLanguage(it.id) }
        assertEquals(setOf("kjv", "asv", "rst", "nrt", "km"), bibles.map { it.id }.toSet())
        for (bible in bibles) {
            assertTrue(bible.translationCode.isNotBlank(), bible.id)
            assertTrue(bible.licence in setOf("public-domain", "proprietary", "unknown"), bible.id)
            assertTrue(bible.attribution.isNotBlank(), bible.id)
            assertFalse("Standart" in bible.displayName, bible.id)
        }
    }

    private fun dump(file: File, table: String): List<List<String?>> =
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM $table ORDER BY 1").use { rs ->
                    buildList {
                        while (rs.next()) add((1..rs.metaData.columnCount).map(rs::getString))
                    }
                }
            }
        }
}
