package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.shared.Position

data class AreaModel(val content: List<DocumentContentModel>, val position: Position?, val interactiveFlowName: String?) : DocumentContentModel {
    override fun collectRefs(): List<RefModel> {
        return content.flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(entity: AreaEntity): AreaModel = AreaModel(
            entity.content.map { DocumentContentModel.fromDbContent(it) }, entity.position, entity.interactiveFlowName
        )
    }
}