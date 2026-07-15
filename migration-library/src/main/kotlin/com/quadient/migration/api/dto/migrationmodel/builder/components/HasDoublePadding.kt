package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasDoublePadding<T> {
    var paddingTop: Double
    var paddingBottom: Double
    var paddingLeft: Double
    var paddingRight: Double

    /**
     * Sets the top padding in pixels.
     * @param v The top padding value.
     * @return This builder instance for method chaining.
     */
    fun paddingTop(v: Double) = apply { paddingTop = v } as T

    /**
     * Sets the bottom padding in pixels.
     * @param v The bottom padding value.
     * @return This builder instance for method chaining.
     */
    fun paddingBottom(v: Double) = apply { paddingBottom = v } as T

    /**
     * Sets the left padding in pixels.
     * @param v The left padding value.
     * @return This builder instance for method chaining.
     */
    fun paddingLeft(v: Double) = apply { paddingLeft = v } as T

    /**
     * Sets the right padding in pixels.
     * @param v The right padding value.
     * @return This builder instance for method chaining.
     */
    fun paddingRight(v: Double) = apply { paddingRight = v } as T

    /**
     * Sets uniform padding on all sides.
     * @param v The padding value to apply to all sides.
     * @return This builder instance for method chaining.
     */
    fun padding(v: Double) = apply { paddingTop = v; paddingBottom = v; paddingLeft = v; paddingRight = v } as T
}
