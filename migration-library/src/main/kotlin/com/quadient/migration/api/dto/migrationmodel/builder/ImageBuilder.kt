package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasMetadata
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSkip
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSourcePath
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasTargetFolder
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions

class ImageBuilder(id: String) : DtoBuilderBase<Image, ImageBuilder>(id),
    HasTargetFolder<ImageBuilder>,
    HasMetadata<ImageBuilder>,
    HasSkip<ImageBuilder>,
    HasSourcePath<ImageBuilder>
{
    override var targetFolder: String? = null
    override var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()
    override var skip = false
    override var placeholder: String? = null
    override var reason: String? = null
    override var sourcePath: String? = null
    var imageType: ImageType? = null
    var options: ImageOptions? = null
    var alternateText: String? = null
    var targetAttachmentId: String? = null

    /**
     * Sets the image options.
     * @param options The [ImageOptions] to set for the image.
     * @return This builder instance for method chaining.
     */
    fun options(options: ImageOptions) = apply { this.options = options }

    /**
     * Sets the file type of the image.
     * @param imageType The [ImageType] of the image.
     * @return This builder instance for method chaining.
     */
    fun imageType(imageType: ImageType) = apply { this.imageType = imageType }

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
     * Sets the target attachment ID for alias resolution.
     * When set, this image reference will resolve to the specified attachment.
     * @param targetAttachmentId the ID of the attachment to resolve to
     * @return the builder instance for chaining
     */
    fun targetAttachmentId(targetAttachmentId: String) = apply { this.targetAttachmentId = targetAttachmentId }

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
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            alternateText = alternateText,
            targetAttachmentId = targetAttachmentId,
        )
    }
}