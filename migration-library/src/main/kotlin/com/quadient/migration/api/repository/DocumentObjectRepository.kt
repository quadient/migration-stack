package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectFilter
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.toDb
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.DocumentObjectTable.baseTemplate
import com.quadient.migration.persistence.table.DocumentObjectTable.content
import com.quadient.migration.persistence.table.DocumentObjectTable.displayRuleRef
import com.quadient.migration.persistence.table.DocumentObjectTable.internal
import com.quadient.migration.persistence.table.DocumentObjectTable.options
import com.quadient.migration.persistence.table.DocumentObjectTable.targetFolder
import com.quadient.migration.persistence.table.DocumentObjectTable.type
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class DocumentObjectRepository(internalRepository: DocumentObjectInternalRepository) :
    Repository<DocumentObject, DocumentObjectModel>(internalRepository) {

    fun list(documentObjectFilter: DocumentObjectFilter): List<DocumentObject> {
        return internalRepository.list(filter(documentObjectFilter)).map(::toDto)
    }

    override fun toDto(model: DocumentObjectModel): DocumentObject = DocumentObject.fromModel(model)

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }
                .filter { it.collectRefs().any { it.id == id } }.map { DocumentObject.fromModel(it) }
                .distinct()
        }
    }

    override fun upsert(dto: DocumentObject) {
        internalRepository.cache.remove(dto.id)
        transaction {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            when (dto.type) {
                DocumentObjectType.Page -> require(dto.options == null || dto.options is PageOptions)
                else -> require(dto.options == null || dto.options !is PageOptions)
            }

            val now = Clock.System.now()

            internalRepository.table.upsert(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[id] = dto.id
                it[projectName] = internalRepository.projectName
                it[type] = dto.type.name
                it[name] = dto.name
                it[content] = dto.content.toDb()
                it[internal] = dto.internal == true
                it[targetFolder] = dto.targetFolder
                it[originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[customFields] = dto.customFields.inner
                it[created] = existingItem?.created ?: now
                it[lastUpdated] = now
                it[displayRuleRef] = dto.displayRuleRef?.id
                it[baseTemplate] = dto.baseTemplate
                it[options] = dto.options
            }
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
            result = result and (type inList filter.types.map { it.toString() })
        }

        return result
    }
}