package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.shared.Position

data class Area(var content: List<DocumentContent>, var position: Position?, var interactiveFlowName: String?, var flowToNextPage: Boolean = false) :
    DocumentContent, RefValidatable {
    
    constructor(content: List<DocumentContent>, position: Position?, interactiveFlowName: String?) : this(content, position, interactiveFlowName, false)
    override fun collectRefs(): List<Ref> {
        return content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptyList()
            }
        }
    }

    companion object {
        fun fromDb(entity: AreaEntity): Area = Area(
            entity.content.map { DocumentContent.fromDbContent(it) }, entity.position, entity.interactiveFlowName, entity.flowToNextPage
        )
    }

    fun toDb() = AreaEntity(content.toDb(), position, interactiveFlowName, flowToNextPage)
}