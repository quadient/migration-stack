package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DataType
import kotlinx.datetime.Instant

data class Variable @JvmOverloads constructor(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var dataType: DataType,
    var defaultValue: String?,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return emptyList()
    }
}