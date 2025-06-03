package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType

class ImageBuilder(id: String) : DtoBuilderBase<Image, ImageBuilder>(id) {
    var sourcePath: String? = null
    var imageType: ImageType? = null
    var options: ImageOptions? = null
    var targetFolder: String? = null

    fun sourcePath(sourcePath: String) = apply { this.sourcePath = sourcePath }
    fun options(options: ImageOptions) = apply { this.options = options }
    fun imageType(imageType: ImageType) = apply { this.imageType = imageType }
    fun targetFolder(targetFolder: String) = apply { this.targetFolder = targetFolder }

    override fun build(): Image {
        return Image(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            sourcePath = sourcePath,
            imageType = imageType,
            options = options,
            targetFolder = targetFolder
        )
    }
}