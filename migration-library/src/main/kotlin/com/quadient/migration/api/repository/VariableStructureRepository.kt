package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class VariableStructureRepository(internalRepository: VariableStructureInternalRepository) :
    Repository<VariableStructure>(internalRepository) {
    override fun toDto(model: VariableStructure): VariableStructure {
        return model
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: VariableStructure) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[VariableStructureTable.id] = dto.id
                it[VariableStructureTable.projectName] = internalRepository.projectName
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
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            this[VariableStructureTable.id] = dto.id
            this[VariableStructureTable.projectName] = internalRepository.projectName
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