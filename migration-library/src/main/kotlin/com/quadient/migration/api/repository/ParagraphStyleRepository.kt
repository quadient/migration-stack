package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.collections.map

class ParagraphStyleRepository(table: ParagraphStyleTable, projectName: String) :
    Repository<ParagraphStyle>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)

    override fun fromDb(row: ResultRow): ParagraphStyle {
        return ParagraphStyle.fromDb(row)
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: ParagraphStyle) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.ParagraphStyle)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[ParagraphStyleTable.id] = dto.id
                it[ParagraphStyleTable.projectName] = projectName
                it[ParagraphStyleTable.name] = dto.name
                it[ParagraphStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[ParagraphStyleTable.customFields] = dto.customFields.inner
                it[ParagraphStyleTable.definition] = dto.definition.toDb()
                it[ParagraphStyleTable.created] = existingItem?.created ?: now
                it[ParagraphStyleTable.lastUpdated] = now
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<ParagraphStyle>) {
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.ParagraphStyle)
            }

            this[ParagraphStyleTable.id] = dto.id
            this[ParagraphStyleTable.projectName] = projectName
            this[ParagraphStyleTable.name] = dto.name
            this[ParagraphStyleTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[ParagraphStyleTable.customFields] = dto.customFields.inner
            this[ParagraphStyleTable.definition] = dto.definition.toDb()
            this[ParagraphStyleTable.created] = existingItem?.created ?: now
            this[ParagraphStyleTable.lastUpdated] = now
        }
    }

    fun firstWithDefinition(id: String): ParagraphStyle? {
        val model = find(id)
        return when (val def = model?.definition) {
            is ParagraphStyleDefinition -> model
            is ParagraphStyleRef -> firstWithDefinition(def.id)
            null -> null
        }
    }
}