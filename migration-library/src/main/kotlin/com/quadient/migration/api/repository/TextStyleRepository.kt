package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class TextStyleRepository(internalRepository: TextStyleInternalRepository) :
    Repository<TextStyle>(internalRepository) {
    val statusTrackingRepository = StatusTrackingRepository(internalRepository.projectName)

    override fun toDto(model: TextStyle): TextStyle {
        return model
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }
                .filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: TextStyle) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.TextStyle)
            }

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[TextStyleTable.id] = dto.id
                it[TextStyleTable.projectName] = internalRepository.projectName
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
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.TextStyle)
            }

            this[TextStyleTable.id] = dto.id
            this[TextStyleTable.projectName] = internalRepository.projectName
            this[TextStyleTable.name] = dto.name
            this[TextStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[TextStyleTable.customFields] = dto.customFields.inner
            this[TextStyleTable.definition] = dto.definition.toDb()
            this[TextStyleTable.created] = existingItem?.created ?: now
            this[TextStyleTable.lastUpdated] = now
        }
    }
}