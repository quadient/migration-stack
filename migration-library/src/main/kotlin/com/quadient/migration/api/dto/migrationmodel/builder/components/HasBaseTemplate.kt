package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasBaseTemplate<T> {
    var baseTemplate: String?

    /**
     * Override the default base template for this object.
     * @param baseTemplate Path to the base template to use for this document object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplate(baseTemplate: String?) = apply { this.baseTemplate = baseTemplate } as T
}
