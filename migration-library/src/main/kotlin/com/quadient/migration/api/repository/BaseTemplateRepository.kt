package com.quadient.migration.api.repository

import com.quadient.migration.api.ProjectName
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.persistence.table.BaseTemplateTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.tools.concat
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import org.jetbrains.exposed.v1.json.extract

class BaseTemplateRepository(
    projectName: ProjectName,
    private val statusTrackingRepository: StatusTrackingRepository,
) : Repository<BaseTemplate>(BaseTemplateTable, projectName.name) {

    override fun fromDb(row: ResultRow): BaseTemplate {
        return BaseTemplate(
            id = row[BaseTemplateTable.id].value,
            name = row[BaseTemplateTable.name],
            customFields = CustomFieldMap(row[BaseTemplateTable.customFields].toMutableMap()),
            lastUpdated = row[BaseTemplateTable.lastUpdated],
            created = row[BaseTemplateTable.created],
            originLocations = row[BaseTemplateTable.originLocations],
            targetFolder = row[BaseTemplateTable.targetFolder],
            pages = row[BaseTemplateTable.pages],
            variableStructureRef = row[BaseTemplateTable.variableStructureRef]?.let { VariableStructureRef(it) },
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where {
                (DocumentObjectTable.projectName eq projectName) and
                    (DocumentObjectTable.baseTemplate.extract<String>("type") eq "BaseTemplateRef") and
                    (DocumentObjectTable.baseTemplate.extract<String>("id") eq id)
            }.map { DocumentObjectTable.fromResultRow(it) }
        }
    }

    override fun upsert(dto: BaseTemplate) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.BaseTemplate)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[BaseTemplateTable.id] = dto.id
                it[BaseTemplateTable.projectName] = this@BaseTemplateRepository.projectName
                it[BaseTemplateTable.name] = dto.name
                it[BaseTemplateTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[BaseTemplateTable.customFields] = dto.customFields.inner
                it[BaseTemplateTable.created] = existingItem?.created ?: now
                it[BaseTemplateTable.lastUpdated] = now
                it[BaseTemplateTable.targetFolder] = dto.targetFolder
                it[BaseTemplateTable.pages] = dto.pages
                it[BaseTemplateTable.variableStructureRef] = dto.variableStructureRef?.id
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<BaseTemplate>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "target_folder", "pages", "variable_structure_ref"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)

                if (existingItem == null) {
                    statusTrackingRepository.active(dto.id, ResourceType.BaseTemplate)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@BaseTemplateRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setString(index++, dto.targetFolder)
                stmt.setObject(index++, Json.encodeToString(dto.pages), Types.OTHER)
                stmt.setString(index++, dto.variableStructureRef?.id)
            }

            stmt.executeUpdate()
        }
    }
}
