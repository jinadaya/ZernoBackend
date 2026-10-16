import data.bible.BibleRepositoryCache
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

fun Application.configureRouting() {
    install(Resources)
    routing {
        get("/health") {
            call.respondText("OK")
        }

        languagesRoutes()
        biblesRoutes()
        booksRoutes()
    }
}

fun Route.languagesRoutes() {
    get("/languages") {
        call.respond(appContext.metaRepository.getLanguages())
    }
}

fun Route.biblesRoutes() {
    get("/bibles") {
        val langId = call.request.queryParameters["langId"] ?: return@get call.respondText(
            "langId is required", status = HttpStatusCode.BadRequest
        )

        call.respond(appContext.metaRepository.getBiblesByLanguage(langId))
    }
}

fun Route.booksRoutes() {

    get("/bible/{bibleId}") {

        val bibleId = call.parameters["bibleId"]!!

        val meta = appContext.metaRepository.getBibleMeta(bibleId)

        val repo = BibleRepositoryCache.getOrCreate(
            meta = meta, factory = appContext.bibleRepositoryFactory
        )

        call.respond(
            repo.getBooks()
        )
    }

    get("/bible/{bibleId}/{bookId}/chapters") {

        val bibleId = call.parameters["bibleId"]!!
        val bookId = call.parameters["bookId"]!!

        val meta = appContext.metaRepository.getBibleMeta(bibleId)

        val repo = BibleRepositoryCache.getOrCreate(
            meta = meta, factory = appContext.bibleRepositoryFactory
        )

        call.respond(repo.getChapters(bookId))
    }

    get("/bible/{bibleId}/verses") {
        val bibleId = call.parameters.getOrFail("bibleId")

        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

        if (limit > 1000) {
            return@get call.respondText(
                "limit must be <= 1000", status = HttpStatusCode.BadRequest
            )
        }

        val meta = appContext.metaRepository.getBibleMeta(bibleId)

        val repo = BibleRepositoryCache.getOrCreate(
            meta = meta, factory = appContext.bibleRepositoryFactory
        )

        call.respond(repo.getVerses(offset, limit))
    }
}
