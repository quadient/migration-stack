package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DisplayRuleDefinition

data class DisplayRule(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: DisplayRuleDefinition?
) : MigrationObject