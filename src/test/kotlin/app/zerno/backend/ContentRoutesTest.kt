package app.zerno.backend

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentRoutesTest {

    @Test
    fun `plans come in index order with a content version`() = fixtureApp {
        val response = client.get("/v1/plans")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val ids = body.getValue("plans").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content }
        assertEquals(listOf("two", "one"), ids)
        val version = body.getValue("version").jsonPrimitive.content
        assertEquals("\"$version\"", response.headers[HttpHeaders.ETag])
        assertEquals("no-cache", response.headers[HttpHeaders.CacheControl])
    }

    @Test
    fun `achievements carry the catalog's schema version`() = fixtureApp {
        val body = Json.parseToJsonElement(client.get("/v1/achievements").bodyAsText()).jsonObject
        assertEquals(1, body.getValue("schema_version").jsonPrimitive.int)
        val codes = body.getValue("achievements").jsonArray.map { it.jsonObject.getValue("code").jsonPrimitive.content }
        assertEquals(listOf("first", "second"), codes)
    }

    @Test
    fun `a known version is not modified`() = fixtureApp {
        val etag = client.get("/v1/plans").headers[HttpHeaders.ETag]!!
        val again = client.get("/v1/plans") { header(HttpHeaders.IfNoneMatch, etag) }
        assertEquals(HttpStatusCode.NotModified, again.status)
        assertTrue(again.bodyAsText().isEmpty())

        val stale = client.get("/v1/plans") { header(HttpHeaders.IfNoneMatch, "\"old\"") }
        assertEquals(HttpStatusCode.OK, stale.status)
    }

    @Test
    fun `plans and achievements have different versions`() = fixtureApp {
        val plans = client.get("/v1/plans").headers[HttpHeaders.ETag]
        val achievements = client.get("/v1/achievements").headers[HttpHeaders.ETag]
        assertTrue(plans != achievements)
    }
}
