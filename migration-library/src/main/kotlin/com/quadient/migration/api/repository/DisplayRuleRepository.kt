package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class DisplayRuleRepository(table: DisplayRuleTable, projectName: String) :
    Repository<DisplayRule>(table, projectName) {

    override fun fromDb(row: ResultRow): DisplayRule {
        return DisplayRule(
            id = row[DisplayRuleTable.id].value,
            name = row[DisplayRuleTable.name],
            originLocations = row[DisplayRuleTable.originLocations],
            customFields = CustomFieldMap(row[DisplayRuleTable.customFields].toMutableMap()),
            lastUpdated = row[DisplayRuleTable.lastUpdated],
            created = row[DisplayRuleTable.created],
            definition = row[DisplayRuleTable.definition]
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
            "created", "last_updated", "definition"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@DisplayRuleRepository.projectName)
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
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            table.upsertReturning(table.id, table.projectName) {
                it[DisplayRuleTable.id] = dto.id
                it[DisplayRuleTable.projectName] = this@DisplayRuleRepository.projectName
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