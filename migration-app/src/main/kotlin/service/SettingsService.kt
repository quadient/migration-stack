package com.quadient.migration.service

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class SettingsService {
    fun getSettings(): Map<String, Map<String, Any?>> {
        val projectConfigDefaults = getClassDefaults(ProjectConfig::class)
        val migConfigDefaults = getClassDefaults(MigConfig::class)
        return mapOf(
            "projectConfig" to projectConfigDefaults, "migrationConfig" to migConfigDefaults
        )
    }

    private fun getClassDefaults(clazz: KClass<*>): Map<String, Any?> {
        val constructor = clazz.primaryConstructor ?: return emptyMap()
        val params = constructor.parameters
        val defaults = mutableMapOf<String, Any?>()
        val instance = constructor.callBy(emptyMap())

        for (param in params) {
            val value = param.name?.let { paramName -> clazz.members.find { it.name == paramName }?.call(instance) }
            defaults[param.name ?: ""] = value
        }
        return defaults
    }
}