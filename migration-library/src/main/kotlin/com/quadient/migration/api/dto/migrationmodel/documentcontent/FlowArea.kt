package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.FlowAreaModel
import com.quadient.migration.persistence.migrationmodel.FlowAreaEntity
import com.quadient.migration.shared.Position

data class FlowArea(val position: Position, val content: List<DocumentContent>) : DocumentContent {
    companion object {
        fun fromModel(model: FlowAreaModel): FlowArea =
            FlowArea(model.position, model.content.map { DocumentContent.fromModelContent(it) })
    }

    fun toDb() = FlowAreaEntity(position, content.toDb())
}