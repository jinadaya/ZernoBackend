package app.zerno.backend

import app.zerno.backend.plugins.configureDependencies
import app.zerno.backend.plugins.configureHTTP
import app.zerno.backend.plugins.configureSerialization
import app.zerno.backend.plugins.configureStatusPages
import app.zerno.backend.routes.configureRouting
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureStatusPages()
    configureDependencies(BibleConfig.from(environment.config))
    configureRouting()
}
