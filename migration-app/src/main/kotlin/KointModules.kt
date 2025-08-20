package com.quadient.migration

import org.koin.dsl.module
import com.quadient.migration.service.SettingsService

val appModules = module {
    single { SettingsService() }
}