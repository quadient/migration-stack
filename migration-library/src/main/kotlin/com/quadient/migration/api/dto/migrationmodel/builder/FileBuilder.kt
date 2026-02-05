package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions

class FileBuilder(id: String) : DtoBuilderBase<File, FileBuilder>(id) {
    var sourcePath: String? = null
    var targetFolder: String? = null
    var fileType: FileType = FileType.Document
    var skip = false
    var placeholder: String? = null
    var reason: String? = null

    /**
     * Sets source path of the file. This path is relative to the storage root folder.
     * @param sourcePath the source path of the file
     * @return the builder instance for chaining
     */
    fun sourcePath(sourcePath: String) = apply { this.sourcePath = sourcePath }

    /**
     * Sets target folder for the file. This is additional folder where the file will be deployed.
     * Supports nesting, e.g. "folder1/folder2".
     * @param targetFolder the target folder for the file
     * @return the builder instance for chaining
     */
    fun targetFolder(targetFolder: String) = apply { this.targetFolder = targetFolder }

    /**
     * Sets the file type (Document or Attachment). Defaults to Document if not specified.
     * @param fileType the type of the file
     * @return the builder instance for chaining
     */
    fun fileType(fileType: FileType) = apply { this.fileType = fileType }

    /**
     * Marks the file to be skipped during deployment.
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
     * Builds the File instance with the provided properties.
     * @return the built File instance
     */
    override fun build(): File {
        return File(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            created = created,
            lastUpdated = lastUpdated,
            sourcePath = sourcePath,
            targetFolder = targetFolder,
            fileType = fileType,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
        )
    }
}
