package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.ShapeEntity
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.ShapePath
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size

data class Shape(
    val name: String?,
    val paths: List<ShapePath>,
    val position: Position,
    val fill: Color?,
    val lineFill: Color?,
    val lineWidth: Size,
    ) : DocumentContent {
    companion object {
        fun fromDb(entity: ShapeEntity): Shape = Shape(
            name = entity.name,
            paths = entity.paths,
            position = entity.position,
            fill = entity.fill,
            lineFill = entity.lineFill,
            lineWidth = entity.lineWidth,
        )
    }

    fun toDb(): ShapeEntity = ShapeEntity(
        name = name,
        paths = paths,
        position = position,
        fill = fill,
        lineFill = lineFill,
        lineWidth = lineWidth,
    )
}
