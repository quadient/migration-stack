package com.quadient.migration.example.common.util

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.Migration
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.shared.IcmPath

import java.nio.charset.StandardCharsets

import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

static Migration initMigration(Binding binding) {
    if (binding.variables["migration"]) {
        return binding.variables["migration"]
    }

    String[] args = binding.variables["args"]

    def classLoader = this.getClassLoader()
    def migConfig = MigConfig.read(classLoader.getResource('migration-config.toml').toURI())

    def argsList = args.any() ? args.toList() : new ArrayList<String>()

    def activeProjectConfig = getValueOfArg("--active-project-config", argsList).orElse(getActiveProjectConfigFromFile(classLoader))
    def fileProjectConfig = ProjectConfig.read(classLoader.getResource(activeProjectConfig).toURI())

    def projectName = getValueOfArg("--project-name", argsList).orElse(fileProjectConfig.name)
    def baseTemplatePath = getValueOfArg("--base-template-path", argsList).orElse(fileProjectConfig.baseTemplatePath)
    def inputDataPath = getValueOfArg("--input-data-path", argsList).orElse(fileProjectConfig.inputDataPath)
    def interactiveTenant = getValueOfArg("--interactive-tenant", argsList).orElse(fileProjectConfig.interactiveTenant)
    def defaultTargetFolder = getValueOfArg("--default-target-folder", argsList).orElse(fileProjectConfig.defaultTargetFolder?.toString())
    def inspireOutput = getValueOfArg("--inspire-output", argsList).orElse(fileProjectConfig.inspireOutput.toString())
    def sourceBaseTemplate = getValueOfArg("--source-base-template-path", argsList).orElse(fileProjectConfig.sourceBaseTemplatePath)
    def defaultVariableStructure = getValueOfArg("--default-variable-structure", argsList).orElse(fileProjectConfig.defaultVariableStructure)

    def defFolder
    if (defaultTargetFolder == null || defaultTargetFolder.isEmpty()) {
        defFolder = null
    } else {
        defFolder = IcmPath.from(defaultTargetFolder)
    }

    def projectConfig = new ProjectConfig(
        projectName,
        baseTemplatePath,
        inputDataPath,
        interactiveTenant,
        defFolder,
        fileProjectConfig.paths,
        InspireOutput.valueOf(inspireOutput),
        sourceBaseTemplate,
        defaultVariableStructure,
        fileProjectConfig.context)
    println("Preparing to start migration script with $projectConfig.")

    return new Migration(migConfig, projectConfig)
}

private static String getActiveProjectConfigFromFile(ClassLoader classLoader) {
    def activeProjectConfigStream = classLoader.getResourceAsStream('active-project-config')
    return activeProjectConfigStream == null ? "project-config.toml" : new String(activeProjectConfigStream.readAllBytes(), StandardCharsets.UTF_8)
}
