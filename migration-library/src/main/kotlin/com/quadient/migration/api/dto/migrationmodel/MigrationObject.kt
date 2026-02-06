package com.quadient.migration.api.dto.migrationmodel

import kotlinx.datetime.Instant

interface MigrationObject {
    val id: String
    var name: String?
    var originLocations: List<String>
    var customFields: CustomFieldMap
    val created: Instant?
    val lastUpdated: Instant?

    fun nameOrId(): String {
        val name = name
        return if (name.isNullOrBlank()) {
            id
        } else {
            name
        }
    }
}