package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.MetadataPrimitive

class ImageBuilder(id: String) : DtoBuilderBase<Image, ImageBuilder>(id) {
    var sourcePath: String? = null
    var imageType: ImageType? = null
    var options: ImageOptions? = null
    var targetFolder: String? = null
    var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()

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
     * Add metadata to the document object.
     * Metadata are not stored if empty.
     * @param key Key of the metadata entry.
     * @param block Builder function where receiver is a [MetadataBuilder].
     * @return This builder instance for method chaining.
     */
    fun metadata(key: String, block: MetadataBuilder.() -> Unit) = apply {
        val result = MetadataBuilder().apply(block).build()
        if (result.isNotEmpty()) {
            metadata[key] = result
        }
    }

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
            targetFolder = targetFolder,
            metadata = metadata,
        )
    }
}