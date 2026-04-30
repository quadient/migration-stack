package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasInternal<T> {
    var internal: Boolean

    /**
     * Set whether the document object is internal. Internal objects do not create a separate
     * file in the target system.
     * @param internal Boolean indicating if the document object is internal.
     * @return This builder instance for method chaining.
     */
    fun internal(internal: Boolean) = apply { this.internal = internal } as T
}
