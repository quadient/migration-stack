package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size

class PositionBuilder {
    private var x: Size = Size.ofCentimeters(0)
    private var y: Size = Size.ofCentimeters(0)
    private var width: Size = Size.ofCentimeters(0)
    private var height: Size = Size.ofCentimeters(0)

    /**
     * Sets the left position in [Size] units.
     * @param x the left position in [Size] units
     * @return the current [PositionBuilder] instance for chaining
     */
    fun left(x: Size) = apply { this.x = x }

    /**
     * Sets the top position in [Size] units.
     * @param y the top position in [Size] units
     * @return the current [PositionBuilder] instance for chaining
     */
    fun top(y: Size) = apply { this.y = y }

    /**
     * Sets the width in [Size] units.
     * @param width the width in [Size] units
     * @return the current [PositionBuilder] instance for chaining
     */
    fun width(width: Size) = apply { this.width = width }

    /**
     * Sets the height in [Size] units.
     * @param height the height in [Size] units
     * @return the current [PositionBuilder] instance for chaining
     */
    fun height(height: Size) = apply { this.height = height }

    /**
     * Builds a [Position] object with the specified dimensions.
     * @return a new [Position] instance
     */
    fun build(): Position {
        return Position(x, y, width, height)
    }
}