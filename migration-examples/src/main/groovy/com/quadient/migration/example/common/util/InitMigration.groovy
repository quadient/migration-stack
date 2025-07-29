package com.quadient.migration.example.common.util

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.Migration
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.shared.IcmPath

import java.nio.charset.StandardCharsets

static Migration initMigration(String[] args) {
    def classLoader = this.getClassLoader()
    def migConfig = MigConfig.read(classLoader.getResource('migration-config.toml').toURI())

    def argsList = args.any() ? args.toList() : new ArrayList<String>()

    def activeProjectConfig = getValueOfArg("--active-project-config", argsList).orElse(getActiveProjectConfigFromFile(classLoader))
    def fileProjectConfig = ProjectConfig.read(classLoader.getResource(activeProjectConfig).toURI())

    def projectName = getValueOfArg("--project-name", argsList).orElse(fileProjectConfig.name)
    def baseTemplatePath = getValueOfArg("--base-template-path", argsList).orElse(fileProjectConfig.baseTemplatePath)
    def inputDataPath = getValueOfArg("--input-data-path", argsList).orElse(fileProjectConfig.inputDataPath)
    def interactiveTenant = getValueOfArg("--interactive-tenant", argsList).orElse(fileProjectConfig.interactiveTenant)
    def defaultTargetFolder = getValueOfArg("--default-target-folder", argsList).orElse(fileProjectConfig.defaultTargetFolder.toString())
    def inspireOutput = getValueOfArg("--inspire-output", argsList).orElse(fileProjectConfig.inspireOutput.toString())
    def baseWfdPath = getValueOfArg("--base-wfd-path", argsList).orElse(fileProjectConfig.baseWfdPath)

    def projectConfig = new ProjectConfig(
        projectName,
        baseTemplatePath,
        inputDataPath,
        interactiveTenant,
        IcmPath.from(defaultTargetFolder),
        fileProjectConfig.paths,
        InspireOutput.valueOf(inspireOutput),
        baseWfdPath,
        fileProjectConfig.context)
    println("Preparing to start migration script with $projectConfig.")

    return new Migration(migConfig, projectConfig)
}

private static String getActiveProjectConfigFromFile(ClassLoader classLoader) {
    def activeProjectConfigStream = classLoader.getResourceAsStream('active-project-config')
    return activeProjectConfigStream == null ? "project-config.toml" : new String(activeProjectConfigStream.readAllBytes(), StandardCharsets.UTF_8)
}

private static Optional<String> getValueOfArg(String arg, List<String> args) {
    def argIndex = args.findIndexOf { it == arg }
    if (argIndex > -1) {
        def argValue = args[argIndex + 1]
        if (argValue == null) {
            println("Value for arg '$arg' is not specified.")
            return Optional.empty()
        } else {
            return Optional.of(argValue)
        }
    }
    return Optional.empty()
}
