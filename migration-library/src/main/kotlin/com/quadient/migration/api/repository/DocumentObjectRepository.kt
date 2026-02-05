package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectFilter
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.toDb
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class DocumentObjectRepository(internalRepository: DocumentObjectInternalRepository) :
    Repository<DocumentObject>(internalRepository) {
    val statusTrackingRepository = StatusTrackingRepository(internalRepository.projectName)

    fun list(documentObjectFilter: DocumentObjectFilter): List<DocumentObject> {
        return internalRepository.list(filter(documentObjectFilter)).map(::toDto)
    }

    override fun toDto(model: DocumentObject): DocumentObject = model

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsertBatch(dtos: Collection<DocumentObject>) {
        internalRepository.upsertBatch(dtos) { dto ->
            val now = Clock.System.now()

            val existingItem = internalRepository.findModel(dto.id)

            when (dto.type) {
                DocumentObjectType.Page -> require(dto.options == null || dto.options is PageOptions)
                else -> require(dto.options == null || dto.options !is PageOptions)
            }

            if ((existingItem == null && dto.internal != true) || (dto.internal == false && existingItem?.internal == true)) {
                statusTrackingRepository.active(
                    dto.id, ResourceType.DocumentObject, mapOf("type" to dto.type.toString())
                )
            }

            this[DocumentObjectTable.id] = dto.id
            this[DocumentObjectTable.projectName] = internalRepository.projectName
            this[DocumentObjectTable.type] = dto.type.name
            this[DocumentObjectTable.name] = dto.name
            this[DocumentObjectTable.content] = dto.content.toDb()
            this[DocumentObjectTable.internal] = dto.internal == true
            this[DocumentObjectTable.targetFolder] = dto.targetFolder
            this[DocumentObjectTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[DocumentObjectTable.customFields] = dto.customFields.inner
            this[DocumentObjectTable.created] = existingItem?.created ?: now
            this[DocumentObjectTable.lastUpdated] = now
            this[DocumentObjectTable.displayRuleRef] = dto.displayRuleRef?.id
            this[DocumentObjectTable.variableStructureRef] = dto.variableStructureRef?.id
            this[DocumentObjectTable.baseTemplate] = dto.baseTemplate
            this[DocumentObjectTable.options] = dto.options
            this[DocumentObjectTable.metadata] = dto.metadata
            this[DocumentObjectTable.skip] = dto.skip
            this[DocumentObjectTable.subject] = dto.subject
        }
    }

    override fun upsert(dto: DocumentObject) {
        internalRepository.upsert {
            val existingItem = internalRepository.findModel(dto.id)

            when (dto.type) {
                DocumentObjectType.Page -> require(dto.options == null || dto.options is PageOptions)
                else -> require(dto.options == null || dto.options !is PageOptions)
            }

            val now = Clock.System.now()

            if ((existingItem == null && dto.internal != true) || (dto.internal == false && existingItem?.internal == true)) {
                statusTrackingRepository.active(
                    dto.id, ResourceType.DocumentObject, mapOf("type" to dto.type.toString())
                )
            }

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[DocumentObjectTable.id] = dto.id
                it[DocumentObjectTable.projectName] = internalRepository.projectName
                it[DocumentObjectTable.type] = dto.type.name
                it[DocumentObjectTable.name] = dto.name
                it[DocumentObjectTable.content] = dto.content.toDb()
                it[DocumentObjectTable.internal] = dto.internal == true
                it[DocumentObjectTable.targetFolder] = dto.targetFolder
                it[DocumentObjectTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[DocumentObjectTable.customFields] = dto.customFields.inner
                it[DocumentObjectTable.created] = existingItem?.created ?: now
                it[DocumentObjectTable.lastUpdated] = now
                it[DocumentObjectTable.displayRuleRef] = dto.displayRuleRef?.id
                it[DocumentObjectTable.variableStructureRef] = dto.variableStructureRef?.id
                it[DocumentObjectTable.baseTemplate] = dto.baseTemplate
                it[DocumentObjectTable.options] = dto.options
                it[DocumentObjectTable.metadata] = dto.metadata
                it[DocumentObjectTable.skip] = dto.skip
                it[DocumentObjectTable.subject] = dto.subject
            }.first()
        }
    }

    private fun filter(filter: DocumentObjectFilter): Op<Boolean> {
        var result = DocumentObjectTable.projectName eq internalRepository.projectName
        if (filter.ids != null && filter.ids.isNotEmpty()) {
            result = result and (DocumentObjectTable.id inList filter.ids)
        }
        if (filter.names != null && filter.names.isNotEmpty()) {
            result = result and (DocumentObjectTable.name.lowerCase() inList filter.names.map { it.lowercase() })
        }
        if (filter.types != null && filter.types.isNotEmpty()) {
            result = result and (DocumentObjectTable.type inList filter.types.map { it.toString() })
        }

        return result
    }
}