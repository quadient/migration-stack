package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSkip
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSourcePath
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasTargetFolder
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions

class AttachmentBuilder(id: String) : DtoBuilderBase<Attachment, AttachmentBuilder>(id),
    HasTargetFolder<AttachmentBuilder>,
    HasSkip<AttachmentBuilder>,
    HasSourcePath<AttachmentBuilder>
{
    override var targetFolder: String? = null
    override var skip = false
    override var placeholder: String? = null
    override var reason: String? = null
    override var sourcePath: String? = null
    var attachmentType: AttachmentType = AttachmentType.Attachment
    var targetImageId: String? = null

    /**
     * Sets the attachment type (Attachment or Document). Defaults to Attachment if not specified.
     * @param attachmentType the type of the attachment
     * @return the builder instance for chaining
     */
    fun attachmentType(attachmentType: AttachmentType) = apply { this.attachmentType = attachmentType }

    /**
     * Sets the target image ID for alias resolution.
     * When set, this attachment reference will resolve to the specified image.
     * @param targetImageId the ID of the image to resolve to
     * @return the builder instance for chaining
     */
    fun targetImageId(targetImageId: String) = apply { this.targetImageId = targetImageId }

    /**
     * Builds the Attachment instance with the provided properties.
     * @return the built Attachment instance
     */
    override fun build(): Attachment {
        return Attachment(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            sourcePath = sourcePath,
            targetFolder = targetFolder,
            attachmentType = attachmentType,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            targetImageId = targetImageId,
        )
    }
}
