package com.quadient.migration.example.common.util

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.Migration
import com.quadient.migration.api.PathsConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.shared.IcmPath
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets

import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

static Migration initMigration(Binding binding) {
    if (binding.variables["migration"]) {
        return binding.variables["migration"]
    }

    String[] args = binding.variables["args"]

    def classLoader = this.getClassLoader()
    def migConfig = MigConfig.read(classLoader.getResource('migration-config.toml').toURI())

    def argsList = args.any() ? args.toList() : new ArrayList<String>()

    def activeProjectConfig = getValueOfArg("--active-project-config", argsList).orElse(getActiveProjectConfigFromFile(classLoader))
    def resource = classLoader.getResource(activeProjectConfig)
    if (resource == null) {
        throw new RuntimeException("Could not find project config file: $activeProjectConfig. Please ensure the file exists in the resources directory.")
    }
    def fileProjectConfig = ProjectConfig.read(resource.toURI())

    def projectName = getValueOfArg("--project-name", argsList).orElse(fileProjectConfig.name)
    def baseTemplatePath = getValueOfArg("--base-template-path", argsList).orElse(fileProjectConfig.baseTemplatePath)
    def styleDefinitionPathArg = getValueOfArg("--style-definition-path", argsList).orElse(fileProjectConfig.styleDefinitionPath?.toString())
    def inputDataPath = getValueOfArg("--input-data-path", argsList).orElse(fileProjectConfig.inputDataPath)
    def interactiveTenant = getValueOfArg("--interactive-tenant", argsList).orElse(fileProjectConfig.interactiveTenant)
    def defaultTargetFolder = getValueOfArg("--default-target-folder", argsList).orElse(fileProjectConfig.defaultTargetFolder?.toString())
    def inspireOutput = getValueOfArg("--inspire-output", argsList).orElse(fileProjectConfig.inspireOutput.toString())
    def sourceBaseTemplate = getValueOfArg("--source-base-template-path", argsList).orElse(fileProjectConfig.sourceBaseTemplatePath)
    def defaultVariableStructure = getValueOfArg("--default-variable-structure", argsList).orElse(fileProjectConfig.defaultVariableStructure)
    def defaultLanguage = getValueOfArg("--default-language", argsList).orElse(fileProjectConfig.defaultLanguage)

    def imagesPathArg = getValueOfArg("--images-path", argsList).orElse(fileProjectConfig.paths.images?.toString())
    def fontsPathArg = getValueOfArg("--fonts-path", argsList).orElse(fileProjectConfig.paths.fonts?.toString())
    def documentsPathArg = getValueOfArg("--documents-path", argsList).orElse(fileProjectConfig.paths.documents?.toString())
    def attachmentsPathArg = getValueOfArg("--attachments-path", argsList).orElse(fileProjectConfig.paths.attachments?.toString())

    def styleDefinitionPath = (styleDefinitionPathArg == null || styleDefinitionPathArg.isEmpty()) ? null : IcmPath.from(styleDefinitionPathArg)
    def defFolder = (defaultTargetFolder == null || defaultTargetFolder.isEmpty()) ? null : IcmPath.from(defaultTargetFolder)
    def imagesPath = (imagesPathArg == null || imagesPathArg.isEmpty()) ? null : IcmPath.from(imagesPathArg)
    def fontsPath = (fontsPathArg == null || fontsPathArg.isEmpty()) ? null : IcmPath.from(fontsPathArg)
    def documentsPath = (documentsPathArg == null || documentsPathArg.isEmpty()) ? null : IcmPath.from(documentsPathArg)
    def attachmentsPath = (attachmentsPathArg == null || attachmentsPathArg.isEmpty()) ? null : IcmPath.from(attachmentsPathArg)

    def projectConfig = new ProjectConfig(projectName,
            baseTemplatePath,
            styleDefinitionPath,
            inputDataPath,
            interactiveTenant,
            defFolder,
            new PathsConfig(imagesPath, fontsPath, documentsPath, attachmentsPath),
            InspireOutput.valueOf(inspireOutput),
            sourceBaseTemplate,
            defaultVariableStructure,
            defaultLanguage,
            fileProjectConfig.context)
    log.info("Preparing to start migration script with $projectConfig.")

    return new Migration(migConfig, projectConfig)
}

private static String getActiveProjectConfigFromFile(ClassLoader classLoader) {
    def activeProjectConfigStream = classLoader.getResourceAsStream('active-project-config')
    return activeProjectConfigStream == null ? "project-config.toml" : new String(activeProjectConfigStream.readAllBytes(), StandardCharsets.UTF_8)
}
