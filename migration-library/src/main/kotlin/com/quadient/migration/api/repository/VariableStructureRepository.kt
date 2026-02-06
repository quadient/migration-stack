package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
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
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "structure", "language_variable"
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
                stmt.setObject(index++, Json.encodeToString(dto.structure), Types.OTHER)
                stmt.setString(index++, dto.languageVariable?.id)
            }

            stmt.executeUpdate()
        }
    }
}