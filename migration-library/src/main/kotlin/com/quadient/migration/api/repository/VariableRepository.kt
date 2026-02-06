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

class VariableRepository(override val table: VariableTable, projectName: String) :
    Repository<Variable>(table, projectName) {

    override fun fromDb(row: ResultRow): Variable {
        return Variable(
            id = row[table.id].value,
            name = row[table.name],
            originLocations = row[table.originLocations],
            customFields = CustomFieldMap(row[table.customFields].toMutableMap()),
            lastUpdated = row[table.lastUpdated],
            created = row[table.created],
            dataType = DataType.valueOf(row[table.dataType]),
            defaultValue = row[table.defaultValue]
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
                it[table.id] = dto.id
                it[table.projectName] = projectName
                it[table.name] = dto.name
                it[table.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[table.customFields] = dto.customFields.inner
                it[table.created] = existingItem?.created ?: now
                it[table.lastUpdated] = now
                it[table.dataType] = dto.dataType.toString()
                it[table.defaultValue] = dto.defaultValue
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<Variable>) {
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            this[VariableTable.id] = dto.id
            this[VariableTable.projectName] = projectName
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