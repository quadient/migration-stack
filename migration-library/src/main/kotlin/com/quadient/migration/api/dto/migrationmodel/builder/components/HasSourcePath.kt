package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasSourcePath<T> {
    var sourcePath: String?
    /**
     * Sets the source path of the attachment. This path is relative to the storage root folder.
     * @param sourcePath The source path of the attachment, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun sourcePath(sourcePath: String?) = apply { this.sourcePath = sourcePath } as T
}

