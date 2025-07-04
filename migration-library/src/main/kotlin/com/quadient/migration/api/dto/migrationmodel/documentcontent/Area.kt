package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.AreaModel
import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.shared.Position

data class Area(val position: Position, val content: List<DocumentContent>) : DocumentContent {
    companion object {
        fun fromModel(model: AreaModel): Area =
            Area(model.position, model.content.map { DocumentContent.fromModelContent(it) })
    }

    fun toDb() = AreaEntity(position, content.toDb())
}