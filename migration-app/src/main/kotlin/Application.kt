package com.quadient.migration

import com.quadient.migration.route.rootModule
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val env = environment.config.getEnv()

    routing {
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

    rootModule()

    log.info("Server started in ${env.name} mode")
}