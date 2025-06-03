package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.FlowAreaEntity
import com.quadient.migration.shared.Position

data class FlowAreaModel(val position: Position, val content: List<DocumentContentModel>) : DocumentContentModel {
    override fun collectRefs(): List<RefModel> {
        return content.flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(entity: FlowAreaEntity): FlowAreaModel =
            FlowAreaModel(entity.position, entity.content.map { DocumentContentModel.fromDbContent(it) })
    }
}