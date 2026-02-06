package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions

class AttachmentBuilder(id: String) : DtoBuilderBase<Attachment, AttachmentBuilder>(id) {
    var sourcePath: String? = null
    var targetFolder: String? = null
    var attachmentType: AttachmentType = AttachmentType.Attachment
    var skip = false
    var placeholder: String? = null
    var reason: String? = null

    /**
     * Sets source path of the attachment. This path is relative to the storage root folder.
     * @param sourcePath the source path of the attachment
     * @return the builder instance for chaining
     */
    fun sourcePath(sourcePath: String) = apply { this.sourcePath = sourcePath }

    /**
     * Sets target folder for the attachment. This is additional folder where the attachment will be deployed.
     * Supports nesting, e.g. "folder1/folder2".
     * @param targetFolder the target folder for the attachment
     * @return the builder instance for chaining
     */
    fun targetFolder(targetFolder: String) = apply { this.targetFolder = targetFolder }

    /**
     * Sets the attachment type (Attachment or Document). Defaults to Attachment if not specified.
     * @param attachmentType the type of the attachment
     * @return the builder instance for chaining
     */
    fun attachmentType(attachmentType: AttachmentType) = apply { this.attachmentType = attachmentType }

    /**
     * Marks the attachment to be skipped during deployment.
     * @param placeholder optional placeholder value to use instead
     * @param reason optional reason for skipping
     * @return the builder instance for chaining
     */
    fun skip(placeholder: String? = null, reason: String? = null) = apply {
        this.skip = true
        this.placeholder = placeholder
        this.reason = reason
    }

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
        )
    }
}
