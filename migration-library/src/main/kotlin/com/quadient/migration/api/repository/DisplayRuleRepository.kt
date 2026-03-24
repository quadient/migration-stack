package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.DocumentObjectTable.variableStructureRef
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class DisplayRuleRepository(table: DisplayRuleTable, projectName: String) :
    Repository<DisplayRule>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)

    override fun fromDb(row: ResultRow): DisplayRule {
        return DisplayRule(
            id = row[DisplayRuleTable.id].value,
            name = row[DisplayRuleTable.name],
            originLocations = row[DisplayRuleTable.originLocations],
            customFields = CustomFieldMap(row[DisplayRuleTable.customFields].toMutableMap()),
            lastUpdated = row[DisplayRuleTable.lastUpdated],
            created = row[DisplayRuleTable.created],
            definition = row[DisplayRuleTable.definition],
            baseTemplate = row[DisplayRuleTable.baseTemplate],
            metadata = row[DisplayRuleTable.metadata],
            subject = row[DisplayRuleTable.subject],
            internal = row[DisplayRuleTable.internal],
            targetId = row[DisplayRuleTable.targetId],
            targetFolder = row[DisplayRuleTable.targetFolder],
            variableStructureRef = row[DisplayRuleTable.variableStructureRef]?.let { VariableStructureRef(it) },
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

    override fun upsertBatch(dtos: Collection<DisplayRule>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "definition",
            "target_id", "internal", "subject", "target_folder",
            "base_template", "variable_structure_ref", "metadata"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)

                if ((existingItem == null && dto.internal != true) || (dto.internal == false && existingItem?.internal == true)) {
                    statusTrackingRepository.active(dto.id, ResourceType.DisplayRule)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@DisplayRuleRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setObject(index++, dto.definition?.let { Json.encodeToString(it) }, Types.OTHER)
                stmt.setString(index++, dto.targetId)
                stmt.setBoolean(index++, dto.internal)
                stmt.setString(index++, dto.subject)
                stmt.setString(index++, dto.targetFolder)
                stmt.setString(index++, dto.baseTemplate)
                stmt.setString(index++, dto.variableStructureRef?.id)
                stmt.setObject(index++, Json.encodeToString(dto.metadata), Types.OTHER)
            }

            stmt.executeUpdate()
        }
    }

    override fun upsert(dto: DisplayRule) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if ((existingItem == null && dto.internal != true) || (dto.internal == false && existingItem?.internal == true)) {
                statusTrackingRepository.active(dto.id, ResourceType.DisplayRule)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[DisplayRuleTable.id] = dto.id
                it[DisplayRuleTable.projectName] = this@DisplayRuleRepository.projectName
                it[DisplayRuleTable.name] = dto.name
                it[DisplayRuleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[DisplayRuleTable.customFields] = dto.customFields.inner
                it[DisplayRuleTable.created] = existingItem?.created ?: now
                it[DisplayRuleTable.lastUpdated] = now
                it[DisplayRuleTable.definition] = dto.definition
                it[DisplayRuleTable.targetId] = dto.targetId
                it[DisplayRuleTable.internal] = dto.internal
                it[DisplayRuleTable.subject] = dto.subject
                it[DisplayRuleTable.targetFolder] = dto.targetFolder
                it[DisplayRuleTable.baseTemplate] = dto.baseTemplate
                it[DisplayRuleTable.variableStructureRef] = dto.variableStructureRef?.id
                it[DisplayRuleTable.metadata] = dto.metadata
            }.first()
        }
    }
}