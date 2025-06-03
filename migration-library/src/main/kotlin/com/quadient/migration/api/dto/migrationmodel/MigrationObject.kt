package com.quadient.migration.api.dto.migrationmodel

interface MigrationObject {
    val id: String
    var name: String?
    var originLocations: List<String>
    var customFields: CustomFieldMap
}