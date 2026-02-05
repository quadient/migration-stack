package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.shared.VariablePathData
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow

data class VariableStructure(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    override val created: Instant,
    override val lastUpdated: Instant,
    val structure: Map<String, VariablePathData>,
    val languageVariable: VariableRef?,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return structure.keys.map { VariableRef(it) }
    }

    companion object {
        fun fromDb(row: ResultRow): VariableStructure {
            return VariableStructure(
                id = row[VariableStructureTable.id].value,
                name = row[VariableStructureTable.name],
                customFields = CustomFieldMap(row[VariableStructureTable.customFields].toMutableMap()),
                lastUpdated = row[VariableStructureTable.lastUpdated],
                created = row[VariableStructureTable.created],
                structure = row[VariableStructureTable.structure],
                originLocations = row[VariableStructureTable.originLocations],
                languageVariable = row[VariableStructureTable.languageVariable]?.let { VariableRef(it) }
            )
        }
    }
}