package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.builder.CategorizationBuilder
import com.quadient.migration.shared.Categorization
import com.quadient.migration.shared.MetadataEntry

@Suppress("UNCHECKED_CAST")
interface HasCategorization<T> {
    var metadata: MutableList<MetadataEntry>

    /**
     * Add a categorization to the object.
     * @param name Name of the categorization.
     * @param block Builder function where receiver is a [CategorizationBuilder].
     * @return This builder instance for method chaining.
     */
    fun categorization(name: String, block: CategorizationBuilder.() -> Unit) = apply {
        metadata.add(Categorization(name, CategorizationBuilder().apply(block).build()))
    } as T

    /**
     * Adds an existing categorization to the object.
     * @param categorization The [Categorization] instance to append.
     * @return This builder instance for method chaining.
     */
    fun categorization(categorization: Categorization): T = apply {
        metadata.add(categorization)
    } as T

    /**
     * Adds an existing categorization to the object, overriding its name.
     * @param name Name of the categorization.
     * @param categorization The [Categorization] instance to append.
     * @return This builder instance for method chaining.
     */
    fun categorization(name: String, categorization: Categorization): T = apply {
        metadata.add(categorization.copy(name = name))
    } as T
}

