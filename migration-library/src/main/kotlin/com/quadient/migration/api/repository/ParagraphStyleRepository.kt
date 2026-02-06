package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Tab
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.service.deploy.ResourceType
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

class ParagraphStyleRepository(table: ParagraphStyleTable, projectName: String) :
    Repository<ParagraphStyle>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)
    override fun fromDb(row: ResultRow): ParagraphStyle {
        val definitionEntity = row[ParagraphStyleTable.definition]
        val definition = when (definitionEntity) {
            is com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity -> {
                ParagraphStyleDefinition(
                    leftIndent = definitionEntity.leftIndent,
                    rightIndent = definitionEntity.rightIndent,
                    defaultTabSize = definitionEntity.defaultTabSize,
                    spaceBefore = definitionEntity.spaceBefore,
                    spaceAfter = definitionEntity.spaceAfter,
                    alignment = definitionEntity.alignment,
                    firstLineIndent = definitionEntity.firstLineIndent,
                    lineSpacing = definitionEntity.lineSpacing,
                    keepWithNextParagraph = definitionEntity.keepWithNextParagraph,
                    tabs = definitionEntity.tabs?.let { tabsEntity ->
                        Tabs(
                            tabs = tabsEntity.tabs.map { Tab(it.position, it.type) },
                            useOutsideTabs = tabsEntity.useOutsideTabs
                        )
                    },
                    pdfTaggingRule = definitionEntity.pdfTaggingRule,
                )
            }
            is com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef -> {
                ParagraphStyleRef.fromDb(definitionEntity)
            }
        }

        return ParagraphStyle(
            id = row[ParagraphStyleTable.id].value,
            name = row[ParagraphStyleTable.name],
            originLocations = row[ParagraphStyleTable.originLocations],
            customFields = CustomFieldMap(row[ParagraphStyleTable.customFields].toMutableMap()),
            lastUpdated = row[ParagraphStyleTable.lastUpdated],
            created = row[ParagraphStyleTable.created],
            definition = definition,
        )
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
                it[ParagraphStyleTable.projectName] = this@ParagraphStyleRepository.projectName
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
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "definition"
        )
        val sql = internalRepository.createSql(columns, dtos.size)
        val now = Clock.System.now()

        internalRepository.upsertBatch(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = internalRepository.findModel(dto.id)

                if (existingItem == null) {
                    statusTrackingRepository.active(dto.id, ResourceType.ParagraphStyle)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, internalRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setObject(index++, Json.encodeToString(dto.definition.toDb()), Types.OTHER)
            }

            stmt.executeUpdate()
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