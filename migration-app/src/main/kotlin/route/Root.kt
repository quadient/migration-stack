package com.quadient.migration.route

import com.quadient.migration.Env
import com.quadient.migration.getEnv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException

fun Application.rootModule() {
    val client = HttpClient()
    val env = environment.config.getEnv()

    routing {
        when (env) {
            Env.DEV -> {
                log.info("Running in development mode, proxying requests to dev server")
                route("/{...}") {
                    get {
                        if (!tryProxyCall(client, call)) {
                            log.trace("Dev server not running, falling back to prebuilt artifacts")

                            log.debug("Serving file for URI: ${call.request.uri}")
                            val file = call.request.toStaticFile()

                            if (!file.exists()) {
                                log.debug("File not found, will try to build frontend")

                                val error = tryBuildFe(call)
                                if (error != null) {
                                    log.error(error.message)
                                    call.respond(HttpStatusCode.InternalServerError, error.message)
                                }

                                log.debug("Successfully built frontend")
                            }

                            call.respondFile(file)
                        }
                    }
                }
            }

            Env.PROD -> {
                staticFiles("/", File("web/dist"))
            }
        }
    }

    log.info("Server started in ${env.name} mode")
}

private suspend fun Application.tryProxyCall(client: HttpClient, call: RoutingCall): Boolean {
    return try {
        val port = environment.config.propertyOrNull("dev.fe-dev-server-port")?.getAs() ?: 5173
        val proxiedResponse = client.get("http://localhost:${port}${call.request.uri}")

        log.trace("Proxying request to http://localhost:${port}${call.request.uri}")
        for ((headerName, headerValues) in proxiedResponse.headers.entries()) {
            for (headerValue in headerValues) {
                call.response.headers.append(headerName, headerValue)
            }
        }

        call.respondText(proxiedResponse.body())

        true
    } catch (_: ConnectException) {
        false
    }
}

@JvmInline
private value class Error(val message: String)

private suspend fun Application.tryBuildFe(call: RoutingCall): Error? {
    return try {
        val (process, result) = withContext(Dispatchers.IO) {
            val process = ProcessBuilder("npm", "run", "build").directory(File("web"))
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
            val result = process.waitFor()
            Pair(process, result)
        }

        if (result != 0) {
            val message =
                "Failed to build frontend, npm returned non-zero exit code: $result and output: ${
                    process.inputStream.bufferedReader().readText()
                }"
            log.error(message)
            call.respond(HttpStatusCode.InternalServerError, message)
            Error(message)
        } else {
            null
        }
    } catch (e: Exception) {
        val message = "Failed to build frontend: ${e.message}"
        log.error(message, e)
        call.respond(HttpStatusCode.InternalServerError, message)
        Error(message)
    }
}

private fun RoutingRequest.toStaticFile() = File(
    if (this.uri == "/") {
        "web/dist/index.html"
    } else {
        // TODO prevent path traversal
        "web/dist${this.uri}"
    }
)

