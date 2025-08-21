package com.quadient.migration.service

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.dto.SettingsResponse

class SettingsService {
    fun getSettings(): SettingsResponse {
        val defaultProjectConfig = ProjectConfig("default-project", "", "", "StandardPackage")
        val defaultMigConfig = MigConfig()

        return SettingsResponse(defaultProjectConfig, defaultMigConfig)
    }
}

