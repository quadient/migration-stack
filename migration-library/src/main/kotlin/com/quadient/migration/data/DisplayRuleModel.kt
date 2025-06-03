package com.quadient.migration.data

import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.DisplayRuleDefinition
import kotlinx.datetime.Instant

data class DisplayRuleModel(
    override val id: String,
    override val name: String? = null,
    override val originLocations: List<String> = emptyList(),
    override val customFields: Map<String, String>,
    override val created: Instant,
    val customFieldsMap: Map<String, String> = emptyMap(),
    val lastUpdated: Instant,
    val definition: DisplayRuleDefinition?
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return definition?.collectRefs() ?: emptyList()
    }
}
