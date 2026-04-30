package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.builder.PositionBuilder
import com.quadient.migration.shared.Position

@Suppress("UNCHECKED_CAST")
interface HasPosition<T> {
    var position: Position?

    /**
     * Sets the position.
     * @param position The [Position] to set.
     * @return This builder instance for method chaining.
     */
    fun position(position: Position?) = apply { this.position = position } as T

    /**
     * Sets the position using a builder function.
     * @param block A builder function to build the [Position].
     * @return This builder instance for method chaining.
     */
    fun position(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        this.position = position
    } as T
}
