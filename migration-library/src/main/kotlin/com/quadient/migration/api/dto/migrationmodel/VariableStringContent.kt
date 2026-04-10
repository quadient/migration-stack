package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.StringEntity
import com.quadient.migration.persistence.migrationmodel.VariableEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableStringContentEntity

sealed interface VariableStringContent : DocumentContent, TextContent, RefValidatable {
    companion object {
        fun fromDb(entity: VariableStringContentEntity): VariableStringContent = when (entity) {
            is StringEntity -> StringValue.fromDb(entity)
            is VariableEntityRef -> VariableRef.fromDb(entity)
        }
    }
}

fun VariableStringContent.toDb(): VariableStringContentEntity = when (this) {
    is StringValue -> this.toDb()
    is VariableRef -> this.toDb()
}