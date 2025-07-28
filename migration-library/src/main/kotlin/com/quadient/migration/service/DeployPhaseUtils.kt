package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.ImageModel
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.isNullOrBlank
import com.quadient.migration.shared.toIcmPath

fun resolveTargetDir(defaultTargetFolder: IcmPath? = null, specificTargetFolder: IcmPath? = null): IcmPath? {
    return when {
        !specificTargetFolder.isNullOrBlank() -> specificTargetFolder
        !defaultTargetFolder.isNullOrBlank() -> defaultTargetFolder
        else -> null
    }
}

fun getBaseTemplateFullPath(config: ProjectConfig, documentObjectBaseTemplatePath: String?): IcmPath {
    val baseTemplatePath = documentObjectBaseTemplatePath ?: config.baseTemplatePath
    val path = baseTemplatePath.toIcmPath()

    if (path.isAbsolute()) return path

    return "icm://Interactive".toIcmPath()
        .join(config.interactiveTenant)
        .join("BaseTemplates")
        .join(path)
}

fun imageExtension(image: ImageModel): String {
    return image.imageType.extension() ?: image.sourcePath?.split('.')?.last() ?: image.name?.split('.')?.last() ?: ""
}