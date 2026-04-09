package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.ColumnLayoutEntity
import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.ColumnBalancingType
import com.quadient.migration.shared.Size

data class ColumnLayout(
    val numberOfColumns: Int,
    val gutterWidth: Size? = null,
    val balancingType: ColumnBalancingType? = null,
    val applyTo: ColumnApplyTo? = null,
    val content: List<DocumentContent>,
) : DocumentContent, RefValidatable {

    override fun collectRefs(): List<Ref> {
        return content.flatMap {
            when (it) {
                is RefValidatable -> it.collectRefs()
                else -> emptyList()
            }
        }
    }

    companion object {
        fun fromDb(entity: ColumnLayoutEntity): ColumnLayout = ColumnLayout(
            numberOfColumns = entity.numberOfColumns,
            gutterWidth = entity.gutterWidth,
            balancingType = entity.balancingType,
            applyTo = entity.applyTo,
            content = entity.content.map { DocumentContent.fromDbContent(it) },
        )
    }

    fun toDb() = ColumnLayoutEntity(
        numberOfColumns = numberOfColumns,
        gutterWidth = gutterWidth,
        balancingType = balancingType,
        applyTo = applyTo,
        content = content.toDb(),
    )
}
