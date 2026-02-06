package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DisplayRuleDefinition
import kotlinx.datetime.Instant

data class DisplayRule @JvmOverloads constructor(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: DisplayRuleDefinition?,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return definition?.collectRefs() ?: emptyList()
    }
}