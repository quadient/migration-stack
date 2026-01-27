package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefOrRef
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class ParagraphStyleRepository(internalRepository: ParagraphStyleInternalRepository) :
    Repository<ParagraphStyle, ParagraphStyleModel>(internalRepository) {
    val statusTrackingRepository = StatusTrackingRepository(internalRepository.projectName)

    override fun toDto(model: ParagraphStyleModel): ParagraphStyle {
        return ParagraphStyle(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            definition = ParagraphStyleDefOrRef.fromModel(model.definition),
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .map { DocumentObject.fromModel(it) }.distinct()
        }
    }

    override fun upsert(dto: ParagraphStyle) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.ParagraphStyle)
            }

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[ParagraphStyleTable.id] = dto.id
                it[ParagraphStyleTable.projectName] = internalRepository.projectName
                it[ParagraphStyleTable.name] = dto.name
                it[ParagraphStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[ParagraphStyleTable.customFields] = dto.customFields.inner
                it[ParagraphStyleTable.definition] = dto.definition.toDb()
                it[ParagraphStyleTable.created] = existingItem?.created ?: now
                it[ParagraphStyleTable.lastUpdated] = now
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<ParagraphStyle>) {
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.ParagraphStyle)
            }

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[ParagraphStyleTable.id] = dto.id
                it[ParagraphStyleTable.projectName] = internalRepository.projectName
                it[ParagraphStyleTable.name] = dto.name
                it[ParagraphStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[ParagraphStyleTable.customFields] = dto.customFields.inner
                it[ParagraphStyleTable.definition] = dto.definition.toDb()
                it[ParagraphStyleTable.created] = existingItem?.created ?: now
                it[ParagraphStyleTable.lastUpdated] = now
            }.first()
        }
    }
}