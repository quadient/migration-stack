package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.EmailOptions

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

@Suppress("UNCHECKED_CAST")
interface HasEmailOptions<T> {
    var options: EmailOptions?

    /**
     * Set options for the email.
     * @param options [EmailOptions] to set for the email.
     * @return This builder instance for method chaining.
     */
    fun options(options: EmailOptions?) = apply { this.options = options } as T
}
