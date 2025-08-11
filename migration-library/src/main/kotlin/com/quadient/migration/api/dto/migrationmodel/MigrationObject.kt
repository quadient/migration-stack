package com.quadient.migration.api.dto.migrationmodel

interface MigrationObject {
    val id: String
    var name: String?
    var originLocations: List<String>
    var customFields: CustomFieldMap

    fun nameOrId(): String {
        val name = name
        return if (name.isNullOrBlank()) {
            id
        } else {
            name
        }
    }
}