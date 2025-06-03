package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefOrRef
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.TextStyleTable.definition
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class TextStyleRepository(internalRepository: TextStyleInternalRepository) :
    Repository<TextStyle, TextStyleModel>(internalRepository) {
    override fun toDto(model: TextStyleModel): TextStyle {
        return TextStyle(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            definition = TextStyleDefOrRef.fromModel(model.definition),
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }
                .filter { it.collectRefs().any { it.id == id } }.map { DocumentObject.fromModel(it) }
                .distinct()
        }
    }

    override fun upsert(dto: TextStyle) {
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
                it[definition] = dto.definition.toDb()
                it[created] = existingItem?.created ?: now
                it[lastUpdated] = now
            }
        }
    }
}