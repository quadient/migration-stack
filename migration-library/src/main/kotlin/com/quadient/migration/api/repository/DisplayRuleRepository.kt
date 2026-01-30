package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.persistence.repository.InternalRepository
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class DisplayRuleRepository(internalRepository: InternalRepository<DisplayRuleModel>) :
    Repository<DisplayRule, DisplayRuleModel>(internalRepository) {
    override fun toDto(model: DisplayRuleModel): DisplayRule {
        return DisplayRule(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            definition = model.definition,
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }
                .filter { it.collectRefs().any { it.id == id } }.map { DocumentObject.fromModel(it) }
                .distinct()
        }
    }

    override fun upsertBatch(dtos: Collection<DisplayRule>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "definition"
        )
        val sql = internalRepository.createSql(columns, dtos.size)
        val now = Clock.System.now()

        internalRepository.upsertBatch(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = internalRepository.findModel(dto.id)

                stmt.setString(index++, dto.id)
                stmt.setString(index++, internalRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setObject(index++, dto.definition?.let { Json.encodeToString(it) }, Types.OTHER)
            }

            stmt.executeUpdate()
        }
    }

    override fun upsert(dto: DisplayRule) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[DisplayRuleTable.id] = dto.id
                it[DisplayRuleTable.projectName] = internalRepository.projectName
                it[DisplayRuleTable.name] = dto.name
                it[DisplayRuleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[DisplayRuleTable.customFields] = dto.customFields.inner
                it[DisplayRuleTable.created] = existingItem?.created ?: now
                it[DisplayRuleTable.lastUpdated] = now
                it[DisplayRuleTable.definition] = dto.definition
            }.first()
        }
    }
}