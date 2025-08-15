package com.quadient.migration

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    routing {
        staticFiles("/", File("web/dist"))

        route("/api") {
            get("/hello") {
                call.respondText("Hello from API!")
            }
        }
    }
}