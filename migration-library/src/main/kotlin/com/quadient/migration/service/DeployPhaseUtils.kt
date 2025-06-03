package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.ImageModel

fun getFolder(projectConfig: ProjectConfig, icmFolder: String? = null): String {
    val folder = when {
        icmFolder != null -> icmFolder
        !projectConfig.defaultTargetFolder.isNullOrBlank() -> projectConfig.defaultTargetFolder
        else -> return ""
    }

    return "${folder.removeSurrounding("/")}/"
}

fun imageExtension(image: ImageModel): String {
    return image.imageType.extension() ?: image.sourcePath?.split('.')?.last() ?: image.name?.split('.')?.last() ?: ""
}