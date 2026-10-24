package app.zerno.backend.routes

import app.zerno.backend.data.bible.BibleRepositories
import app.zerno.backend.domain.meta.MetaRepository
import app.zerno.backend.plugins.BadRequestException
import app.zerno.backend.plugins.NotFoundException
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject

const val MAX_VERSES_LIMIT = 1000

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respondText("OK")
        }

        languagesRoutes()
        biblesRoutes()
        bibleRoutes()
        contentRoutes()
    }
}

fun Route.languagesRoutes() {
    val metaRepository by inject<MetaRepository>()

    get("/languages") {
        call.respond(metaRepository.getLanguages())
    }
}

fun Route.biblesRoutes() {
    val metaRepository by inject<MetaRepository>()

    get("/bibles") {
        val langId = call.request.queryParameters["langId"]
            ?: throw BadRequestException("langId is required")
        metaRepository.getLanguage(langId)
            ?: throw NotFoundException("language_not_found", "Unknown language '$langId'")

        call.respond(metaRepository.getBiblesByLanguage(langId))
    }
}

fun Route.bibleRoutes() {
    val bibles by inject<BibleRepositories>()

    get("/bible/{bibleId}") {
        val repo = bibles.get(call.parameters.getOrFail("bibleId"))
        call.respond(repo.getBooks())
    }

    get("/bible/{bibleId}/{bookId}/chapters") {
        val repo = bibles.get(call.parameters.getOrFail("bibleId"))
        val bookId = call.parameters.getOrFail("bookId")
        val bookNumber = bookId.toIntOrNull()
            ?: throw BadRequestException("bookId must be a book number, got '$bookId'")

        val chapters = repo.getChapters(bookNumber)
        if (chapters.isEmpty()) throw NotFoundException("book_not_found", "Unknown book '$bookId'")
        call.respond(chapters)
    }

    get("/bible/{bibleId}/verses") {
        val offset = call.intQueryParameter("offset", default = 0)
        val limit = call.intQueryParameter("limit", default = 50)
        if (offset < 0) throw BadRequestException("offset must be >= 0")
        if (limit !in 1..MAX_VERSES_LIMIT) throw BadRequestException("limit must be between 1 and $MAX_VERSES_LIMIT")

        val repo = bibles.get(call.parameters.getOrFail("bibleId"))
        call.respond(repo.getVerses(offset, limit))
    }
}

private fun ApplicationCall.intQueryParameter(name: String, default: Int): Int {
    val raw = request.queryParameters[name] ?: return default
    return raw.toIntOrNull() ?: throw BadRequestException("$name must be a number, got '$raw'")
}
