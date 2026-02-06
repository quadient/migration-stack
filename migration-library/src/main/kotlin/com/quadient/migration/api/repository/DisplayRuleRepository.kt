package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
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
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            this[DisplayRuleTable.id] = dto.id
            this[DisplayRuleTable.projectName] = projectName
            this[DisplayRuleTable.name] = dto.name
            this[DisplayRuleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[DisplayRuleTable.customFields] = dto.customFields.inner
            this[DisplayRuleTable.created] = existingItem?.created ?: now
            this[DisplayRuleTable.lastUpdated] = now
            this[DisplayRuleTable.definition] = dto.definition
        }
    }

    override fun upsert(dto: DisplayRule) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            table.upsertReturning(table.id, table.projectName) {
                it[DisplayRuleTable.id] = dto.id
                it[DisplayRuleTable.projectName] = projectName
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