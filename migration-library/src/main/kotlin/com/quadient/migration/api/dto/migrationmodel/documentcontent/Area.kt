package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.shared.Position

data class Area @JvmOverloads constructor(
    var content: List<DocumentContent>,
    var position: Position?,
    var interactiveFlowName: String?,
    var flowToNextPage: Boolean = false,
    var name: String? = null,
    var dynamicHeight: Boolean = false,
) :
    DocumentContent, RefValidatable {
    override val pathName = "area"

    override fun toPreview(nameResolver: (DocumentContent) -> String?): String =
        "$pathName: ${content.size} items"

    override fun collectRefs(): Set<Ref> {
        return content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptySet()
            }
        }.toSet()
    }

    companion object {
        fun fromDb(entity: AreaEntity): Area = Area(
            entity.content.map { DocumentContent.fromDbContent(it) },
            entity.position,
            entity.interactiveFlowName,
            entity.flowToNextPage,
            entity.name,
            entity.dynamicHeight,
        )
    }

    fun toDb() = AreaEntity(content.toDb(), position, interactiveFlowName, flowToNextPage, name, dynamicHeight)
}