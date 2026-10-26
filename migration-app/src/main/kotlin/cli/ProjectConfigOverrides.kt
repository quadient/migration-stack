package com.quadient.migration.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.shared.IcmPath

class ProjectConfigOverrides : OptionGroup(
    name = "Project config overrides",
    help = "Override values of the active project config for this run",
) {
    private val projectName by option("--project-name")
    private val baseTemplatePath by option("--base-template-path")
    private val styleDefinitionPath by option("--style-definition-path")
    private val inputDataPath by option("--input-data-path")
    private val interactiveTenant by option("--interactive-tenant")
    private val defaultTargetFolder by option("--default-target-folder")
    private val inspireOutput by option("--inspire-output").enum<InspireOutput>()
    private val sourceBaseTemplatePath by option("--source-base-template-path")
    private val defaultVariableStructure by option("--default-variable-structure")
    private val defaultLanguage by option("--default-language")
    private val selectedDocumentObjects by option(
        "--selected-document-objects",
        help = "Comma separated document object ids, empty value selects all",
    ).convert { value -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
    private val subProjectId by option("--sub-project-id")
    private val imagesPath by option("--images-path")
    private val fontsPath by option("--fonts-path")
    private val documentsPath by option("--documents-path")
    private val attachmentsPath by option("--attachments-path")

    fun applyTo(config: ProjectConfig): ProjectConfig = config.copy(
        name = projectName ?: config.name,
        baseTemplatePath = baseTemplatePath ?: config.baseTemplatePath,
        styleDefinitionPath = styleDefinitionPath.toIcmPath(config.styleDefinitionPath),
        inputDataPath = inputDataPath ?: config.inputDataPath,
        interactiveTenant = interactiveTenant ?: config.interactiveTenant,
        defaultTargetFolder = defaultTargetFolder.toIcmPath(config.defaultTargetFolder),
        paths = config.paths.copy(
            images = imagesPath.toIcmPath(config.paths.images),
            fonts = fontsPath.toIcmPath(config.paths.fonts),
            documents = documentsPath.toIcmPath(config.paths.documents),
            attachments = attachmentsPath.toIcmPath(config.paths.attachments),
        ),
        inspireOutput = inspireOutput ?: config.inspireOutput,
        sourceBaseTemplatePath = sourceBaseTemplatePath ?: config.sourceBaseTemplatePath,
        defaultVariableStructure = defaultVariableStructure ?: config.defaultVariableStructure,
        defaultLanguage = defaultLanguage ?: config.defaultLanguage,
        selectedDocumentObjects = selectedDocumentObjects ?: config.selectedDocumentObjects,
        subProjectId = subProjectId ?: config.subProjectId,
    )
}

private fun String?.toIcmPath(current: IcmPath?): IcmPath? = when {
    this == null -> current
    isEmpty() -> null
    else -> IcmPath.from(this)
}
