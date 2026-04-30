package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasTargetFolder<T> {
    var targetFolder: String?

    /**
     * Set the target folder for the object.
     * @param targetFolder String representing the target folder path.
     * @return This builder instance for method chaining.
     */
    fun targetFolder(targetFolder: String?) = apply { this.targetFolder = targetFolder } as T
}
