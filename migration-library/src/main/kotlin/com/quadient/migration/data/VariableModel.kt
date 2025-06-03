package com.quadient.migration.data

import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.DataType
import kotlinx.datetime.Instant

data class VariableModel(
    override val id: String,
    override val name: String? = null,
    override val originLocations: List<String> = emptyList(),
    override val customFields: Map<String, String>,
    override val created: Instant,
    val lastUpdated: Instant,
    val dataType: DataType,
    val defaultValue: String?
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return emptyList()
    }
}
