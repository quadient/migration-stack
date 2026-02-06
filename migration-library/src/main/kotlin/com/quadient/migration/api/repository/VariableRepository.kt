package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableTable
import com.quadient.migration.shared.DataType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class VariableRepository(table: VariableTable, projectName: String) :
    Repository<Variable>(table, projectName) {

    override fun fromDb(row: ResultRow): Variable {
        return Variable(
            id = row[VariableTable.id].value,
            name = row[VariableTable.name],
            originLocations = row[VariableTable.originLocations],
            customFields = CustomFieldMap(row[VariableTable.customFields].toMutableMap()),
            lastUpdated = row[VariableTable.lastUpdated],
            created = row[VariableTable.created],
            dataType = DataType.valueOf(row[VariableTable.dataType]),
            defaultValue = row[VariableTable.defaultValue]
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: Variable) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            table.upsertReturning(table.id, table.projectName) {
                it[VariableTable.id] = dto.id
                it[VariableTable.projectName] = this@VariableRepository.projectName
                it[VariableTable.name] = dto.name
                it[VariableTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[VariableTable.customFields] = dto.customFields.inner
                it[VariableTable.created] = existingItem?.created ?: now
                it[VariableTable.lastUpdated] = now
                it[VariableTable.dataType] = dto.dataType.toString()
                it[VariableTable.defaultValue] = dto.defaultValue
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<Variable>) {
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            this[VariableTable.id] = dto.id
            this[VariableTable.projectName] = this@VariableRepository.projectName
            this[VariableTable.name] = dto.name
            this[VariableTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[VariableTable.customFields] = dto.customFields.inner
            this[VariableTable.created] = existingItem?.created ?: now
            this[VariableTable.lastUpdated] = now
            this[VariableTable.dataType] = dto.dataType.toString()
            this[VariableTable.defaultValue] = dto.defaultValue
        }
    }
}