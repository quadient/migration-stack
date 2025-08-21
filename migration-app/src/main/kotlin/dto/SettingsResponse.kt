package com.quadient.migration.dto

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import kotlinx.serialization.Serializable

@Serializable
data class SettingsResponse(
    val projectConfig: ProjectConfig,
    val migrationConfig: MigConfig,
)