package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectFilter
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.toDb
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import java.sql.Types

class DocumentObjectRepository(table: DocumentObjectTable, projectName: String) :
    Repository<DocumentObject>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)

    val logger = org.slf4j.LoggerFactory.getLogger(DocumentObjectRepository::class.java)!!

    fun list(documentObjectFilter: DocumentObjectFilter): List<DocumentObject> {
        return list(filterByDocumentObjectFilter(documentObjectFilter))
    }

    override fun fromDb(row: ResultRow): DocumentObject = DocumentObjectTable.fromResultRow(row)

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }


    override fun upsertBatch(dtos: Collection<DocumentObject>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "type", "name", "content", "internal", "target_folder",
            "origin_locations", "custom_fields", "created", "last_updated", "display_rule_ref",
            "variable_structure_ref", "base_template", "options", "metadata", "skip", "subject"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)
                when (dto.type) {
                    DocumentObjectType.Page -> require(dto.options == null || dto.options is PageOptions)
                    else -> require(dto.options == null || dto.options !is PageOptions)
                }

                if ((existingItem == null && dto.internal != true) || (dto.internal == false && existingItem?.internal == true)) {
                    statusTrackingRepository.active(
                        dto.id, ResourceType.DocumentObject, mapOf("type" to dto.type.toString())
                    )
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@DocumentObjectRepository.projectName)
                stmt.setString(index++, dto.type.name)
                stmt.setString(index++, dto.name)
                stmt.setObject(index++, Json.encodeToString(dto.content.toDb()), Types.OTHER)
                stmt.setBoolean(index++, dto.internal == true)
                stmt.setString(index++, dto.targetFolder)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setString(index++, dto.displayRuleRef?.id)
                stmt.setString(index++, dto.variableStructureRef?.id)
                stmt.setString(index++, dto.baseTemplate)
                stmt.setObject(index++, dto.options?.let { Json.encodeToString(it) }, Types.OTHER)
                stmt.setObject(index++, dto.metadata.let { Json.encodeToString(it) }, Types.OTHER)
                stmt.setObject(index++, Json.encodeToString(dto.skip), Types.OTHER)
                stmt.setString(index++, dto.subject)
            }

            stmt.executeUpdate()
        }
    }

    override fun upsert(dto: DocumentObject) {
        upsertInternal {
            val existingItem = find(dto.id)

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

            table.upsertReturning(table.id, table.projectName) {
                it[DocumentObjectTable.id] = dto.id
                it[DocumentObjectTable.projectName] = this@DocumentObjectRepository.projectName
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

    private fun filterByDocumentObjectFilter(filter: DocumentObjectFilter): Op<Boolean> {
        var result = DocumentObjectTable.projectName eq projectName
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