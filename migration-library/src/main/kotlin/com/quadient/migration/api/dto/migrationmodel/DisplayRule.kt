package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.shared.DisplayRuleDefinition
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow

data class DisplayRule @JvmOverloads constructor(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: DisplayRuleDefinition?,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return definition?.collectRefs() ?: emptyList()
    }

    companion object {
        fun fromDb(row: ResultRow): DisplayRule {
            return DisplayRule(
                id = row[DisplayRuleTable.id].value,
                name = row[DisplayRuleTable.name],
                originLocations = row[DisplayRuleTable.originLocations],
                customFields = CustomFieldMap(row[DisplayRuleTable.customFields].toMutableMap()),
                lastUpdated = row[DisplayRuleTable.lastUpdated],
                created = row[DisplayRuleTable.created],
                definition = row[DisplayRuleTable.definition]
            )
        }
    }
}