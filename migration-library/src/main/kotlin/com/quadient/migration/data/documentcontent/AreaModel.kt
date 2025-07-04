package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.shared.Position

data class AreaModel(val position: Position, val content: List<DocumentContentModel>) : DocumentContentModel {
    override fun collectRefs(): List<RefModel> {
        return content.flatMap { it.collectRefs() }
    }

    companion object {
        fun fromDb(entity: AreaEntity): AreaModel =
            AreaModel(entity.position, entity.content.map { DocumentContentModel.fromDbContent(it) })
    }
}