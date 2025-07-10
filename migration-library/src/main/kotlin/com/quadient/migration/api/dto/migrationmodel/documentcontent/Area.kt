package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.AreaModel
import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.shared.Position

data class Area(var content: List<DocumentContent>, var position: Position?, var interactiveFlowName: String?) :
    DocumentContent {
    companion object {
        fun fromModel(model: AreaModel): Area =
            Area(model.content.map { DocumentContent.fromModelContent(it) }, model.position, model.interactiveFlowName)
    }

    fun toDb() = AreaEntity(content.toDb(), position, interactiveFlowName)
}