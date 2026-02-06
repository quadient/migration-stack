package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class VariableStructureRepository(table: VariableStructureTable, projectName: String) :
    Repository<VariableStructure>(table, projectName) {

    override fun fromDb(row: ResultRow): VariableStructure {
        return VariableStructure(
            id = row[VariableStructureTable.id].value,
            name = row[VariableStructureTable.name],
            customFields = CustomFieldMap(row[VariableStructureTable.customFields].toMutableMap()),
            lastUpdated = row[VariableStructureTable.lastUpdated],
            created = row[VariableStructureTable.created],
            structure = row[VariableStructureTable.structure],
            originLocations = row[VariableStructureTable.originLocations],
            languageVariable = row[VariableStructureTable.languageVariable]?.let { VariableRef(it) }
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: VariableStructure) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            table.upsertReturning(table.id, table.projectName) {
                it[VariableStructureTable.id] = dto.id
                it[VariableStructureTable.projectName] = this@VariableStructureRepository.projectName
                it[VariableStructureTable.name] = dto.name
                it[VariableStructureTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[VariableStructureTable.customFields] = dto.customFields.inner
                it[VariableStructureTable.created] = existingItem?.created ?: now
                it[VariableStructureTable.lastUpdated] = now
                it[VariableStructureTable.structure] = dto.structure
                it[VariableStructureTable.languageVariable] = dto.languageVariable?.id
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<VariableStructure>) {
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            this[VariableStructureTable.id] = dto.id
            this[VariableStructureTable.projectName] = this@VariableStructureRepository.projectName
            this[VariableStructureTable.name] = dto.name
            this[VariableStructureTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[VariableStructureTable.customFields] = dto.customFields.inner
            this[VariableStructureTable.created] = existingItem?.created ?: now
            this[VariableStructureTable.lastUpdated] = now
            this[VariableStructureTable.structure] = dto.structure
            this[VariableStructureTable.languageVariable] = dto.languageVariable?.id
        }
    }
}