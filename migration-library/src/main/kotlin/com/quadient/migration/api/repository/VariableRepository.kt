package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.data.VariableModel
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class VariableRepository(internalRepository: VariableInternalRepository) :
    Repository<Variable, VariableModel>(internalRepository) {
    override fun toDto(model: VariableModel): Variable {
        return Variable(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            dataType = model.dataType,
            defaultValue = model.defaultValue
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .map { DocumentObject.fromModel(it) }.distinct()
        }
    }

    override fun upsert(dto: Variable) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[VariableTable.id] = dto.id
                it[VariableTable.projectName] = internalRepository.projectName
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
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "data_type", "default_value"
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
                stmt.setString(index++, dto.dataType.toString())
                stmt.setString(index++, dto.defaultValue)
            }

            stmt.executeUpdate()
        }
    }
}