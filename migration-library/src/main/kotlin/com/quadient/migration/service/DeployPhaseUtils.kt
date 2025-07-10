package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.ImageModel
import kotlin.text.removePrefix

fun getFolder(projectConfig: ProjectConfig, icmFolder: String? = null): String {
    val folder = when {
        icmFolder != null -> icmFolder
        !projectConfig.defaultTargetFolder.isNullOrBlank() -> projectConfig.defaultTargetFolder
        else -> return ""
    }

    return "${folder.removePrefix("/").removeSuffix("/")}/"
}

fun getBaseTemplateFullPath(config: ProjectConfig, documentObjectBaseTemplatePath: String?): String {
    val baseTemplatePath = documentObjectBaseTemplatePath ?: config.baseTemplatePath

    val normalizedPath = baseTemplatePath.replace("\\", "/").replace("vcs", "icm")

    val isFullPath = normalizedPath.startsWith("icm://")
    if (isFullPath) return normalizedPath

    val relativePath = normalizedPath.removePrefix("/").removeSuffix("/")
    return "icm://Interactive/${config.interactiveTenant}/BaseTemplates/$relativePath"
}

fun imageExtension(image: ImageModel): String {
    return image.imageType.extension() ?: image.sourcePath?.split('.')?.last() ?: image.name?.split('.')?.last() ?: ""
}