package com.quadient.migration.service

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import org.slf4j.LoggerFactory

private const val LEGACY_SETTINGS_FILE = "settings.json"
private const val ACTIVE_CONFIG_FILE = "active-config.json"
private val log = LoggerFactory.getLogger(SettingsService::class.java)

class SettingsService(
    private val fileStorageService: FileStorageService,
    private val configFiles: ConfigFileService,
) {
    init {
        importLegacySettings()
        getActive()
    }

    val activeProject: String
        get() = configFiles.readProject(getActive().projectConfig).name

    @Synchronized
    fun getSettings(): Settings {
        val active = getActive()
        return Settings(
            projectConfig = configFiles.readProject(active.projectConfig),
            migrationConfig = configFiles.readMigration(active.migrationConfig),
            sourceFormat = active.sourceFormats[active.projectConfig],
        )
    }

    @Synchronized
    fun setSettings(settings: Settings) {
        val active = getActive()
        configFiles.writeProject(active.projectConfig, settings.projectConfig)
        configFiles.writeMigration(active.migrationConfig, settings.migrationConfig)
    }

    @Synchronized
    fun setSourceFormat(sourceFormat: String?) = saveActive(getActive().withSourceFormat(sourceFormat))

    @Synchronized
    fun getActive(): ActiveConfig {
        val stored = fileStorageService.readAppJson<ActiveConfig>(ACTIVE_CONFIG_FILE)
        val active = ActiveConfig(
            projectConfig = existingOrDefault(ConfigKind.Project, stored?.projectConfig),
            migrationConfig = existingOrDefault(ConfigKind.Migration, stored?.migrationConfig),
            sourceFormats = stored?.sourceFormats ?: emptyMap(),
        )
        if (active != stored) {
            saveActive(active)
        }
        return active
    }

    @Synchronized
    fun select(projectConfig: String?, migrationConfig: String?) {
        projectConfig?.let { requireExists(ConfigKind.Project, it) }
        migrationConfig?.let { requireExists(ConfigKind.Migration, it) }
        val active = getActive()
        saveActive(
            active.copy(
                projectConfig = projectConfig ?: active.projectConfig,
                migrationConfig = migrationConfig ?: active.migrationConfig,
            )
        )
    }

    @Synchronized
    fun createConfig(kind: ConfigKind, name: String, cloneFrom: String?, template: String?) {
        configFiles.create(kind, name, cloneFrom, template)

        if (kind == ConfigKind.Project && cloneFrom != null) {
            val active = getActive()
            active.sourceFormats[cloneFrom]?.let { saveActive(active.copy(sourceFormats = active.sourceFormats + (name to it))) }
        }
    }

    private fun importLegacySettings() {
        if (configFiles.list(ConfigKind.Project).isNotEmpty()) {
            return
        }
        val legacy = fileStorageService.readAppJson<Settings>(LEGACY_SETTINGS_FILE) ?: return
        val name = legacy.projectConfig.name

        configFiles.writeProject(name, legacy.projectConfig)
        if (!configFiles.exists(ConfigKind.Migration, name)) {
            configFiles.writeMigration(name, legacy.migrationConfig)
        }
        saveActive(ActiveConfig(name, name).withSourceFormat(legacy.sourceFormat))
    }

    private fun existingOrDefault(kind: ConfigKind, name: String?): String {
        if (name != null && configFiles.exists(kind, name)) {
            return name
        }
        val names = configFiles.list(kind)
        return when {
            names.isEmpty() -> DEFAULT_NAME.also { createDefault(kind) }
            DEFAULT_NAME in names -> DEFAULT_NAME
            else -> names.first()
        }
    }

    private fun createDefault(kind: ConfigKind) {
        val template = DEFAULT_NAME.takeIf { it in configFiles.listTemplates(kind) }
        try {
            configFiles.create(kind, DEFAULT_NAME, cloneFrom = null, template = template)
        } catch (e: ConfigFileException) {
            log.warn("Creating config '$DEFAULT_NAME' with built-in defaults: ${e.message}")
            configFiles.create(kind, DEFAULT_NAME, cloneFrom = null)
        }
    }

    private fun requireExists(kind: ConfigKind, name: String) {
        if (!configFiles.exists(kind, name)) {
            throw NoSuchElementException("Config '$name' not found")
        }
    }

    private fun saveActive(active: ActiveConfig) = fileStorageService.writeAppJson(active, ACTIVE_CONFIG_FILE)
}

data class ActiveConfig(
    val projectConfig: String,
    val migrationConfig: String,
    val sourceFormats: Map<String, String> = emptyMap(),
) {
    fun withSourceFormat(sourceFormat: String?): ActiveConfig {
        val others = sourceFormats - projectConfig
        return copy(sourceFormats = if (sourceFormat == null) others else others + (projectConfig to sourceFormat))
    }
}

data class Settings(
    val projectConfig: ProjectConfig,
    val migrationConfig: MigConfig,
    val sourceFormat: String? = null,
)
