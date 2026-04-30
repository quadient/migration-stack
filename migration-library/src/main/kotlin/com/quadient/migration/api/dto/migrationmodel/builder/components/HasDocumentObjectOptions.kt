package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.shared.DocumentObjectOptions

@Suppress("UNCHECKED_CAST")
interface HasDocumentObjectOptions<T> {
    var options: DocumentObjectOptions?

    /**
     * Set options for the document object.
     * @param options [DocumentObjectOptions] to set for the document object.
     * @return This builder instance for method chaining.
     */
    fun options(options: DocumentObjectOptions?) = apply { this.options = options } as T
}
