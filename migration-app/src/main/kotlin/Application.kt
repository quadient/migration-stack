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
            route("/settings") {
                get {
                    println("Received request for settings")
                    call.respondText("Settings API Endpoint")
                }
            }

            route("/tasks") {
                get {
                    println("Received request for tasks")
                    call.respondText("Tasks API Endpoint")
                }
            }
        }
    }
}