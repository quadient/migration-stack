package com.quadient.migration.service

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import kotlinx.serialization.Serializable

class SettingsService {
    private var settings: Settings = initSettings()

    private fun initSettings(): Settings {
        val defaultProjectConfig = ProjectConfig("default-project", "", "", "StandardPackage")
        val defaultMigConfig = MigConfig()

        return Settings(defaultProjectConfig, defaultMigConfig)
    }

    fun getSettings(): Settings {
        return settings
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
    }
}

@Serializable
data class Settings(
    val projectConfig: ProjectConfig,
    val migrationConfig: MigConfig,
)