package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef

@Suppress("UNCHECKED_CAST")
interface HasTextStyleRef<T> {
    var styleRef: TextStyleRef?

    /**
     * Sets the text style reference.
     * @param styleRef The [TextStyleRef] to set.
     * @return This builder instance for method chaining.
     */
    fun styleRef(styleRef: TextStyleRef?) = apply { this.styleRef = styleRef } as T

    /**
     * Sets the text style reference using a string ID.
     * @param styleRefId The ID of the text style to reference.
     * @return This builder instance for method chaining.
     */
    fun styleRef(styleRefId: String?) = apply { this.styleRef = styleRefId?.let{ TextStyleRef(it) } } as T

    /**
     * Sets the text style reference using a [TextStyle] model object.
     * @param style The text style whose ID will be used as the reference.
     * @return This builder instance for method chaining.
     */
    fun styleRef(style: TextStyle?) = apply { this.styleRef = style?. let { TextStyleRef(it.id) } } as T
}
