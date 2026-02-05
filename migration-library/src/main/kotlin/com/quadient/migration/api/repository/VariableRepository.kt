package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class VariableRepository(internalRepository: VariableInternalRepository) :
    Repository<Variable>(internalRepository) {
    override fun toDto(model: Variable): Variable {
        return model
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
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
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            this[VariableTable.id] = dto.id
            this[VariableTable.projectName] = internalRepository.projectName
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