package com.quadient.migration.api.dto.migrationmodel

interface RefValidatable {
    fun collectRefs(): List<Ref>
}
