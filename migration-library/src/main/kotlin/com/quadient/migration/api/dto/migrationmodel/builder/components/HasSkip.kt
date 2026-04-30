package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasSkip<T> {
    var skip: Boolean
    var placeholder: String?
    var reason: String?

    /**
     * Marks the object as skipped, optionally providing a placeholder and reason.
     * @param placeholder Optional placeholder text to use instead of the skipped content.
     * @param reason Optional reason explaining why this object is being skipped.
     * @return This builder instance for method chaining.
     */
    fun skip(placeholder: String? = null, reason: String? = null) = apply {
        this.skip = true
        this.placeholder = placeholder
        this.reason = reason
    } as T
}
