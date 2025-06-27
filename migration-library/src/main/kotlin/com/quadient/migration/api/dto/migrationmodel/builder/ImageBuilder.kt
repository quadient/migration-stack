package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType

class ImageBuilder(id: String) : DtoBuilderBase<Image, ImageBuilder>(id) {
    var sourcePath: String? = null
    var imageType: ImageType? = null
    var options: ImageOptions? = null
    var targetFolder: String? = null

    /**
     * Sets source path of the image. This path is relative to the storage root folder.
     * @param sourcePath the source path of the image
     * @return the builder instance for chaining
     */
    fun sourcePath(sourcePath: String) = apply { this.sourcePath = sourcePath }

    /**
     * Sets the name of the image.
     * @param name the name of the image
     * @return the builder instance for chaining
     */
    fun options(options: ImageOptions) = apply { this.options = options }

    /**
     * Sets the name of the image.
     * @param imageType the filetype of the image
     * @return the builder instance for chaining
     */
    fun imageType(imageType: ImageType) = apply { this.imageType = imageType }

    /**
     * Sets target folder for the image. This is additional folder where the image will be deployed.
     * Supports nesting, e.g. "folder1/folder2".
     * @param targetFolder the target folder for the image
     * @return the builder instance for chaining
     */
    fun targetFolder(targetFolder: String) = apply { this.targetFolder = targetFolder }

    /**
     * Builds the Image instance with the provided properties.
     * @return the built Image instance
     */
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