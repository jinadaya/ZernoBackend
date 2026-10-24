package app.zerno.backend

import app.zerno.backend.model.Translation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationRoutesTest {

    private val tstFile = File(TestFixtures.dir, "bibles/en/tst.SQLite3")
    private val tstSha = MessageDigest.getInstance("SHA-256").digest(tstFile.readBytes())
        .joinToString("") { "%02x".format(it) }

    @Test
    fun `the catalog lists translations with a file and a licence`() = fixtureApp {
        val response = client.get("/v1/translations")
        assertEquals(HttpStatusCode.OK, response.status)
        val translations = response.translations()
        // `gone` has no file on the server and `lck` has no licence yet.
        assertEquals(listOf("tst"), translations.map { it.id })
        val tst = translations.single()
        assertEquals("TST", tst.translationCode)
        assertEquals("public-domain", tst.licence)
        assertEquals("Test attribution", tst.attribution)
        assertEquals(1, tst.schemaVersion)
        assertEquals(tstFile.length(), tst.size)
        assertEquals(tstSha, tst.sha256)
        assertEquals(tstSha.take(16), tst.fileVersion)
        assertEquals("v1/translations/tst/file", tst.downloadUrl)
        assertEquals(false, tst.bundled)
    }

    @Test
    fun `the catalog is versioned like the other v1 documents`() = fixtureApp {
        val etag = client.get("/v1/translations").headers[HttpHeaders.ETag]!!
        val again = client.get("/v1/translations") { header(HttpHeaders.IfNoneMatch, etag) }
        assertEquals(HttpStatusCode.NotModified, again.status)
    }

    @Test
    fun `unlicensed translations show up when development asks for them`() = testApplication {
        environment { config = TestFixtures.config().apply { put("translations.showUnlicensed", "true") } }
        application { module() }
        assertEquals(listOf("lck", "tst"), client.get("/v1/translations").translations().map { it.id })
    }

    @Test
    fun `the file downloads whole with its checksum as the ETag`() = fixtureApp {
        val response = client.get("/v1/translations/tst/file")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContentEquals(tstFile.readBytes(), response.readRawBytes())
        assertEquals("\"$tstSha\"", response.headers[HttpHeaders.ETag])
        assertEquals("bytes", response.headers[HttpHeaders.AcceptRanges])
        assertTrue(response.headers[HttpHeaders.ContentDisposition]!!.contains("tst.SQLite3"))
    }

    @Test
    fun `a dropped download resumes from a byte offset`() = fixtureApp {
        val bytes = tstFile.readBytes()
        val response = client.get("/v1/translations/tst/file") {
            header(HttpHeaders.Range, "bytes=100-")
            header(HttpHeaders.IfRange, "\"$tstSha\"")
        }
        assertEquals(HttpStatusCode.PartialContent, response.status)
        assertEquals("bytes 100-${bytes.size - 1}/${bytes.size}", response.headers[HttpHeaders.ContentRange])
        assertContentEquals(bytes.copyOfRange(100, bytes.size), response.readRawBytes())
    }

    @Test
    fun `a resume against a changed file starts over`() = fixtureApp {
        val response = client.get("/v1/translations/tst/file") {
            header(HttpHeaders.Range, "bytes=100-")
            header(HttpHeaders.IfRange, "\"an-older-file\"")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(tstFile.length(), response.readRawBytes().size.toLong())
    }

    @Test
    fun `a known file is not modified`() = fixtureApp {
        val response = client.get("/v1/translations/tst/file") { header(HttpHeaders.IfNoneMatch, "\"$tstSha\"") }
        assertEquals(HttpStatusCode.NotModified, response.status)
    }

    @Test
    fun `missing, unlicensed and unknown translations are not found`() = fixtureApp {
        for (id in listOf("gone", "lck", "nope")) {
            val response = client.get("/v1/translations/$id/file")
            assertEquals(HttpStatusCode.NotFound, response.status, id)
            val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error").toString()
            assertEquals("\"translation_not_found\"", error, id)
        }
    }

    private suspend fun HttpResponse.translations(): List<Translation> {
        val body = Json.parseToJsonElement(bodyAsText()).jsonObject.getValue("translations")
        return Json.decodeFromJsonElement(body)
    }
}
