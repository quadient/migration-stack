package com.quadient.migration.route

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.service.ActiveConfig
import com.quadient.migration.service.ConfigFileException
import com.quadient.migration.service.ConfigFileMissingException
import com.quadient.migration.service.ConfigFileService
import com.quadient.migration.service.ConfigKind
import com.quadient.migration.service.RunLock
import com.quadient.migration.service.Settings
import com.quadient.migration.service.SettingsService
import com.quadient.migration.withPermitOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configsModule() {
    val settingsService by inject<SettingsService>()
    val configFiles by inject<ConfigFileService>()
    val runLock by inject<RunLock>()

    routing { configsRoutes(settingsService, configFiles, runLock) }
}

fun Route.configsRoutes(settingsService: SettingsService, configFiles: ConfigFileService, runLock: RunLock) {
    route("/api/settings") {
        get {
            try {
                val active = settingsService.getActive()
                call.respond(SettingsPayload.from(settingsService.getSettings(), active))
            } catch (e: ConfigFileException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.UnprocessableEntity)
            }
        }

        post {
            val payload = call.receive<SettingsPayload>()
            val active = settingsService.getActive()
            if (payload.activeProjectConfig != active.projectConfig || payload.activeMigrationConfig != active.migrationConfig) {
                call.respondText(
                    "The selected configs changed since Settings was opened, reopen Settings",
                    status = HttpStatusCode.Conflict
                )
                return@post
            }
            try {
                settingsService.setSettings(payload.toSettings())
                call.respondText("Settings saved successfully")
            } catch (e: ConfigFileException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.UnprocessableEntity)
            }
        }
    }

    route("/api/configs") {
        get {
            val active = settingsService.getActive()
            call.respond(
                ConfigsResponse(
                    projectConfigs = configFiles.list(ConfigKind.Project),
                    migrationConfigs = configFiles.list(ConfigKind.Migration),
                    projectTemplates = configFiles.listTemplates(ConfigKind.Project),
                    migrationTemplates = configFiles.listTemplates(ConfigKind.Migration),
                    activeProjectConfig = active.projectConfig,
                    activeMigrationConfig = active.migrationConfig,
                )
            )
        }

        put("/active") {
            val request = call.receive<SelectConfigRequest>()
            runLock.withPermitOrElse(onUnavailable = {
                call.respondText(
                    "A module is running, try again when it finishes",
                    status = HttpStatusCode.Conflict
                )
            }) {
                try {
                    settingsService.select(request.projectConfig, request.migrationConfig)
                    call.respond(HttpStatusCode.OK)
                } catch (e: NoSuchElementException) {
                    call.respondText(e.message.orEmpty(), status = HttpStatusCode.NotFound)
                }
            }
        }

        put("/source-format") {
            settingsService.setSourceFormat(call.receive<SourceFormatRequest>().sourceFormat)
            call.respond(HttpStatusCode.OK)
        }

        post("/{kind}") {
            val request = call.receive<CreateConfigRequest>()
            val kind = when (call.parameters["kind"]) {
                "project" -> ConfigKind.Project
                "migration" -> ConfigKind.Migration
                else -> {
                    call.respondText("Unknown config kind", status = HttpStatusCode.NotFound)
                    return@post
                }
            }

            try {
                settingsService.createConfig(kind, request.name, request.cloneFrom, request.template)
                call.respond(HttpStatusCode.Created)
            } catch (e: IllegalStateException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.Conflict)
            } catch (e: ConfigFileMissingException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.NotFound)
            } catch (e: ConfigFileException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.UnprocessableEntity)
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }
    }
}

data class SettingsPayload(
    val projectConfig: ProjectConfig,
    val migrationConfig: MigConfig,
    val sourceFormat: String?,
    val activeProjectConfig: String,
    val activeMigrationConfig: String,
) {
    fun toSettings() = Settings(projectConfig, migrationConfig, sourceFormat)

    companion object {
        fun from(settings: Settings, active: ActiveConfig) = SettingsPayload(
            settings.projectConfig,
            settings.migrationConfig,
            settings.sourceFormat,
            active.projectConfig,
            active.migrationConfig,
        )
    }
}

data class ConfigsResponse(
    val projectConfigs: List<String>,
    val migrationConfigs: List<String>,
    val projectTemplates: List<String>,
    val migrationTemplates: List<String>,
    val activeProjectConfig: String,
    val activeMigrationConfig: String,
)

data class SelectConfigRequest(val projectConfig: String? = null, val migrationConfig: String? = null)

data class SourceFormatRequest(val sourceFormat: String? = null)

data class CreateConfigRequest(val name: String, val cloneFrom: String? = null, val template: String? = null)
