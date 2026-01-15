package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefOrRef
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ParagraphStyleTable.definition
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

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
        internalRepository.upsert(dto.id) {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.ParagraphStyle)
            }

            internalRepository.table.upsert(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[id] = dto.id
                it[projectName] = internalRepository.projectName
                it[name] = dto.name
                it[originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[customFields] = dto.customFields.inner
                it[definition] = dto.definition.toDb()
                it[created] = existingItem?.created ?: now
                it[lastUpdated] = now
            }
        }
    }
}