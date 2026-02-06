package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.table.VariableTable
import com.quadient.migration.shared.DataType
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow

data class Variable @JvmOverloads constructor(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var dataType: DataType,
    var defaultValue: String?,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return emptyList()
    }

    companion object {
        fun fromDb(row: ResultRow): Variable {
            return Variable(
                id = row[VariableTable.id].value,
                name = row[VariableTable.name],
                originLocations = row[VariableTable.originLocations],
                customFields = CustomFieldMap(row[VariableTable.customFields].toMutableMap()),
                lastUpdated = row[VariableTable.lastUpdated],
                created = row[VariableTable.created],
                dataType = DataType.valueOf(row[VariableTable.dataType]),
                defaultValue = row[VariableTable.defaultValue]
            )
        }
    }
}