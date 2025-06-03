package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.DocumentObjectType

data class DocumentObjectFilter(
    val ids: List<String>?,
    val names: List<String>?,
    val types: List<DocumentObjectType>?,
)
