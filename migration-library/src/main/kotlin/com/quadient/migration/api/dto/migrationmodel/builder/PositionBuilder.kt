package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size

class PositionBuilder {
    private var x: Size = Size.ofCentimeters(0)
    private var y: Size = Size.ofCentimeters(0)
    private var width: Size = Size.ofCentimeters(0)
    private var height: Size = Size.ofCentimeters(0)

    fun left(x: Size) = apply { this.x = x }
    fun top(y: Size) = apply { this.y = y }
    fun width(width: Size) = apply { this.width = width }
    fun height(height: Size) = apply { this.height = height }
    fun build(): Position {
        return Position(x, y, width, height)
    }
}