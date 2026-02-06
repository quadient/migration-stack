package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class TextStyleRepository(table: TextStyleTable, projectName: String) :
    Repository<TextStyle>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)

    override fun fromDb(row: ResultRow): TextStyle {
        val definitionEntity = row[TextStyleTable.definition]
        val definition = when (definitionEntity) {
            is com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity -> {
                TextStyleDefinition(
                    fontFamily = definitionEntity.fontFamily,
                    foregroundColor = definitionEntity.foregroundColor,
                    size = definitionEntity.size,
                    bold = definitionEntity.bold,
                    italic = definitionEntity.italic,
                    underline = definitionEntity.underline,
                    strikethrough = definitionEntity.strikethrough,
                    superOrSubscript = definitionEntity.superOrSubscript,
                    interspacing = definitionEntity.interspacing,
                )
            }
            is com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef -> {
                TextStyleRef.fromDb(definitionEntity)
            }
        }
        
        return TextStyle(
            id = row[TextStyleTable.id].value,
            name = row[TextStyleTable.name],
            originLocations = row[TextStyleTable.originLocations],
            customFields = CustomFieldMap(row[TextStyleTable.customFields].toMutableMap()),
            lastUpdated = row[TextStyleTable.lastUpdated],
            created = row[TextStyleTable.created],
            definition = definition,
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }
                .filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: TextStyle) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.TextStyle)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[TextStyleTable.id] = dto.id
                it[TextStyleTable.projectName] = projectName
                it[TextStyleTable.name] = dto.name
                it[TextStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[TextStyleTable.customFields] = dto.customFields.inner
                it[TextStyleTable.definition] = dto.definition.toDb()
                it[TextStyleTable.created] = existingItem?.created ?: now
                it[TextStyleTable.lastUpdated] = now
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<TextStyle>) {
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.TextStyle)
            }

            this[TextStyleTable.id] = dto.id
            this[TextStyleTable.projectName] = projectName
            this[TextStyleTable.name] = dto.name
            this[TextStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[TextStyleTable.customFields] = dto.customFields.inner
            this[TextStyleTable.definition] = dto.definition.toDb()
            this[TextStyleTable.created] = existingItem?.created ?: now
            this[TextStyleTable.lastUpdated] = now
        }
    }

    fun firstWithDefinition(id: String): TextStyle? {
        val model = find(id)
        return when (val def = model?.definition) {
            is TextStyleDefinition -> model
            is TextStyleRef -> firstWithDefinition(def.id)
            null -> null
        }
    }
}