package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.repository.InternalRepository
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class DisplayRuleRepository(internalRepository: InternalRepository<DisplayRule>) :
    Repository<DisplayRule>(internalRepository) {
    override fun toDto(model: DisplayRule): DisplayRule {
        return model
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }
                .filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsertBatch(dtos: Collection<DisplayRule>) {
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            this[DisplayRuleTable.id] = dto.id
            this[DisplayRuleTable.projectName] = internalRepository.projectName
            this[DisplayRuleTable.name] = dto.name
            this[DisplayRuleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[DisplayRuleTable.customFields] = dto.customFields.inner
            this[DisplayRuleTable.created] = existingItem?.created ?: now
            this[DisplayRuleTable.lastUpdated] = now
            this[DisplayRuleTable.definition] = dto.definition
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