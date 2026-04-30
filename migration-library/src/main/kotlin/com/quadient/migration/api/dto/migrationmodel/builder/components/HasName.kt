package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasName<T> {
    var name: String?

    /**
     * Sets an optional name for the element being built.
     * @param name The name to assign.
     * @return This builder instance for method chaining.
     */
    fun name(name: String?) = apply { this.name = name } as T
}
