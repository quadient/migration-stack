package com.quadient.migration.data

import kotlinx.datetime.Instant

interface MigrationObjectModel {
    val id: String
    val name: String?
    val originLocations: List<String>
    val customFields: Map<String, String>
    val created: Instant

    fun nameOrId(): String {
        val name = name
        return if (name.isNullOrBlank()) {
            id
        } else {
            name
        }
    }
}