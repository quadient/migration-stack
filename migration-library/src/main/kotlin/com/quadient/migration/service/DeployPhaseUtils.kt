package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
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

fun imageExtension(image: Image) = imageExtension(image.imageType!!, image.name, image.sourcePath)

fun imageExtension(imageType: ImageType, name: String?, sourcePath: String?): String {
    return imageType.extension() ?: sourcePath?.split('.')?.last() ?: name?.split('.')?.last() ?: ""
}