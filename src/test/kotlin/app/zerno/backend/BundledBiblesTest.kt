package app.zerno.backend

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/** Smoke test against the real files in `databases/` with the shipped config. */
class BundledBiblesTest {

    @Test
    fun `bundled bibles open with 66 books`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        for (id in listOf("kjv", "asv", "rst")) {
            val response = client.get("/bible/$id")
            assertEquals(HttpStatusCode.OK, response.status, "$id: ${response.bodyAsText()}")
            val books = Json.parseToJsonElement(response.bodyAsText()).jsonObject["books"]!!.jsonArray
            assertEquals(66, books.size, id)
            assertEquals(50, books.first().jsonObject["chapter_count"]!!.jsonPrimitive.int, "$id Genesis")
        }
    }
}
