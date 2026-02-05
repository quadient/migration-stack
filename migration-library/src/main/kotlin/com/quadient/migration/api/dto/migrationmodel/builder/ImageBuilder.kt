package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions

class ImageBuilder(id: String) : DtoBuilderBase<Image, ImageBuilder>(id) {
    var sourcePath: String? = null
    var imageType: ImageType? = null
    var options: ImageOptions? = null
    var targetFolder: String? = null
    var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()
    var skip = false
    var placeholder: String? = null
    var reason: String? = null
    var alternateText: String? = null

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

    fun skip(placeholder: String? = null, reason: String? = null) = apply {
        this.skip = true
        this.placeholder = placeholder
        this.reason = reason
    }

    /**
     * Sets the subject of the document object. This is visible as description in Interactive
     * The subject is part of the metadata with key "Subject" and this is just a shorthand for
     * metadata("Subject") { string(subject) }
     * @param subject the subject of the document object
     * @return the builder instance for chaining
     */
    fun subject(subject: String) = apply {
        metadata("Subject") { string(subject) }
    }

    /**
     * Sets the alternate text for the image for accessibility purposes.
     * @param alternateText the alternate text for the image
     * @return the builder instance for chaining
     */
    fun alternateText(alternateText: String) = apply { this.alternateText = alternateText }

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
            created = created,
            lastUpdated = lastUpdated,
            sourcePath = sourcePath,
            imageType = imageType,
            options = options,
            targetFolder = targetFolder,
            metadata = metadata,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            alternateText = alternateText,
        )
    }
}