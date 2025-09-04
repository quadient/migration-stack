package com.quadient.migration.service

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import kotlinx.serialization.Serializable

private const val STORAGE_FILE = "settings.json"

class SettingsService(val fileStorageService: FileStorageService) {
    private var settings: Settings = initSettings()
    val activeProject: String
        get() = settings.projectConfig.name

    private fun initSettings(): Settings {
        fileStorageService.readAppJson<Settings>(STORAGE_FILE)?.let { return it }

        val defaultProjectConfig = ProjectConfig("default-project", "", "", "StandardPackage")
        val defaultMigConfig = MigConfig()

        return Settings(defaultProjectConfig, defaultMigConfig)
    }

    fun getSettings(): Settings {
        return settings
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
        fileStorageService.writeAppJson(settings, STORAGE_FILE)
    }
}

@Serializable
data class Settings(
    val projectConfig: ProjectConfig,
    val migrationConfig: MigConfig,
    val sourceFormat: String? = null,
)