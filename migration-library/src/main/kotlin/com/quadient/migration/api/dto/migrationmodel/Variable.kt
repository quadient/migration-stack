package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DataType

data class Variable(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var dataType: DataType,
    var defaultValue: String?
) : MigrationObject