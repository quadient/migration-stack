package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.builder.MetadataBuilder
import com.quadient.migration.shared.MetadataPrimitive
import kotlin.collections.set

@Suppress("UNCHECKED_CAST")
interface HasMetadata<T> {
    var metadata: MutableMap<String, List<MetadataPrimitive>>

    /**
     * Add metadata to the object.
     * Metadata are not stored if empty.
     * @param key Key of the metadata entry.
     * @param block Builder function where receiver is a [MetadataBuilder].
     * @return This builder instance for method chaining.
     */
    fun metadata(key: String, block: MetadataBuilder.() -> Unit) = apply {
        val result = MetadataBuilder().apply(block).build()
        if (result.isNotEmpty()) {
            metadata[key] = result
        }
    } as T
}
