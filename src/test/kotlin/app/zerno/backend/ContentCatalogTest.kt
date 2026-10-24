package app.zerno.backend

import app.zerno.backend.content.ContentCatalog
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ContentCatalogTest {

    private fun contentDir(): File = Files.createTempDirectory("zerno-content").toFile().also(TestFixtures::writeContent)

    @Test
    fun `the shipped content loads`() {
        val catalog = ContentCatalog(File("content"))
        assertEquals(3, catalog.plans.body.getValue("plans").jsonArray.size)
        assertEquals(37, catalog.achievements.body.getValue("achievements").jsonArray.size)
    }

    @Test
    fun `a changed file changes the version`() {
        val dir = contentDir()
        val before = ContentCatalog(dir).plans.version
        File(dir, "plans/one.json").writeText(
            """{"id": "one", "version": 2, "days": [{"day": 1, "passages": ["John 2"], "minutes": 5}]}""",
        )
        assertNotEquals(before, ContentCatalog(dir).plans.version)
    }

    @Test
    fun `a plan missing from disk fails at start`() {
        val dir = contentDir()
        File(dir, "plans/index.json").writeText("""["one", "three"]""")
        assertFailsWith<IllegalArgumentException> { ContentCatalog(dir) }
    }

    @Test
    fun `a plan whose id does not match its file fails at start`() {
        val dir = contentDir()
        File(dir, "plans/one.json").writeText("""{"id": "uno", "days": [{"day": 1}]}""")
        assertFailsWith<IllegalArgumentException> { ContentCatalog(dir) }
    }

    @Test
    fun `a duplicate achievement code fails at start`() {
        val dir = contentDir()
        File(dir, "achievements.json").writeText("""{"version": 1, "achievements": [{"code": "a"}, {"code": "a"}]}""")
        assertFailsWith<IllegalArgumentException> { ContentCatalog(dir) }
    }
}
