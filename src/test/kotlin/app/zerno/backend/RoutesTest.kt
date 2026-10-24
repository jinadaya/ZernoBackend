package app.zerno.backend

import app.zerno.backend.model.Bible
import app.zerno.backend.model.BibleBooksResponse
import app.zerno.backend.model.ChapterInfo
import app.zerno.backend.model.ErrorResponse
import app.zerno.backend.model.Language
import app.zerno.backend.model.VersesPage
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {

    @Test
    fun `health responds OK`() = fixtureApp {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun `languages lists every language`() = fixtureApp {
        val languages = client.get("/languages").decode<List<Language>>()
        assertEquals(listOf("en", "xx"), languages.map { it.id })
    }

    @Test
    fun `bibles lists a language's bibles with licence and attribution`() = fixtureApp {
        val bibles = client.get("/bibles?langId=en").decode<List<Bible>>()
        assertEquals(listOf("gone", "lck", "tst"), bibles.map { it.id })
        val tst = bibles.single { it.id == "tst" }
        assertEquals("TST", tst.translationCode)
        assertEquals("public-domain", tst.licence)
        assertEquals("Test attribution", tst.attribution)
    }

    @Test
    fun `bibles is empty for a language without bibles`() = fixtureApp {
        assertEquals(emptyList(), client.get("/bibles?langId=xx").decode<List<Bible>>())
    }

    @Test
    fun `bibles without langId is a bad request`() = fixtureApp {
        client.get("/bibles").assertError(HttpStatusCode.BadRequest, "bad_request")
    }

    @Test
    fun `bibles for an unknown language is not found`() = fixtureApp {
        client.get("/bibles?langId=zz").assertError(HttpStatusCode.NotFound, "language_not_found")
    }

    @Test
    fun `bible lists books in order with chapter counts`() = fixtureApp {
        val bible = client.get("/bible/tst").decode<BibleBooksResponse>()
        assertEquals("Test Bible", bible.displayName)
        assertEquals(listOf("10", "20"), bible.books.map { it.id })
        assertEquals(listOf(1, 2), bible.books.map { it.order })
        assertEquals(listOf(2, 1), bible.books.map { it.chapterCount })
        assertEquals("Genesis", bible.books.first().displayName)
    }

    @Test
    fun `unknown bible is not found`() = fixtureApp {
        client.get("/bible/nope").assertError(HttpStatusCode.NotFound, "bible_not_found")
        client.get("/bible/nope/10/chapters").assertError(HttpStatusCode.NotFound, "bible_not_found")
        client.get("/bible/nope/verses").assertError(HttpStatusCode.NotFound, "bible_not_found")
    }

    @Test
    fun `bible without a file on the server is not found`() = fixtureApp {
        client.get("/bible/gone").assertError(HttpStatusCode.NotFound, "bible_file_missing")
    }

    @Test
    fun `chapters takes the book id from the books list`() = fixtureApp {
        val chapters = client.get("/bible/tst/10/chapters").decode<List<ChapterInfo>>()
        assertEquals(
            listOf(ChapterInfo("10:1", 1, 3), ChapterInfo("10:2", 2, 1)),
            chapters,
        )
    }

    @Test
    fun `chapters of an unknown book is not found`() = fixtureApp {
        client.get("/bible/tst/999/chapters").assertError(HttpStatusCode.NotFound, "book_not_found")
    }

    @Test
    fun `chapters with a non-numeric book is a bad request`() = fixtureApp {
        client.get("/bible/tst/gen/chapters").assertError(HttpStatusCode.BadRequest, "bad_request")
    }

    @Test
    fun `verses pages through the whole bible in reading order`() = fixtureApp {
        val first = client.get("/bible/tst/verses?offset=0&limit=4").decode<VersesPage>()
        assertEquals(6, first.total)
        assertEquals(
            listOf("Genesis 1:1", "Genesis 1:2", "Genesis 1:3", "Genesis 2:1"),
            first.verses.map { it.text },
        )
        val second = client.get("/bible/tst/verses?offset=4&limit=4").decode<VersesPage>()
        assertEquals(listOf("Exodus 1:1", "Exodus 1:2"), second.verses.map { it.text })
        assertEquals(20, second.verses.first().book)
    }

    @Test
    fun `verses defaults to the first 50`() = fixtureApp {
        val page = client.get("/bible/tst/verses").decode<VersesPage>()
        assertEquals(0, page.offset)
        assertEquals(50, page.limit)
        assertEquals(6, page.verses.size)
    }

    @Test
    fun `verses rejects bad paging`() = fixtureApp {
        for (query in listOf("limit=1001", "limit=0", "offset=-1", "limit=many")) {
            client.get("/bible/tst/verses?$query").assertError(HttpStatusCode.BadRequest, "bad_request")
        }
    }

    @Test
    fun `unknown route is a JSON 404`() = fixtureApp {
        client.get("/nope").assertError(HttpStatusCode.NotFound, "not_found")
    }
}

private val json = Json { ignoreUnknownKeys = true }

private suspend inline fun <reified T> HttpResponse.decode(): T {
    assertEquals(HttpStatusCode.OK, status, bodyAsText())
    return json.decodeFromString(bodyAsText())
}

private suspend fun HttpResponse.assertError(expected: HttpStatusCode, code: String) {
    assertEquals(expected, status, bodyAsText())
    assertEquals(code, json.decodeFromString<ErrorResponse>(bodyAsText()).error)
}
