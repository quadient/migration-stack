package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.data.VariableModel
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.VariableTable.dataType
import com.quadient.migration.persistence.table.VariableTable.defaultValue
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

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
        internalRepository.cache.remove(dto.id)
        transaction {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            internalRepository.table.upsert(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[id] = dto.id
                it[projectName] = internalRepository.projectName
                it[name] = dto.name
                it[originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[customFields] = dto.customFields.inner
                it[created] = existingItem?.created ?: now
                it[lastUpdated] = now
                it[dataType] = dto.dataType.toString()
                it[defaultValue] = dto.defaultValue
            }
        }
    }
}