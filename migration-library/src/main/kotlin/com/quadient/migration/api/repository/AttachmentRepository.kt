package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.AttachmentTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import java.sql.Types

class AttachmentRepository(table: AttachmentTable, projectName: String) : Repository<Attachment>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)
    override fun fromDb(row: ResultRow): Attachment {
        return Attachment(
            id = row[AttachmentTable.id].value,
            name = row[AttachmentTable.name],
            originLocations = row[AttachmentTable.originLocations],
            customFields = CustomFieldMap(row[AttachmentTable.customFields].toMutableMap()),
            created = row[AttachmentTable.created],
            lastUpdated = row[AttachmentTable.created],
            sourcePath = row[AttachmentTable.sourcePath],
            targetFolder = row[AttachmentTable.targetFolder],
            attachmentType = AttachmentType.valueOf(row[AttachmentTable.attachmentType]),
            skip = row[AttachmentTable.skip],
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: Attachment) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.Attachment)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[AttachmentTable.id] = dto.id
                it[AttachmentTable.projectName] = this@AttachmentRepository.projectName
                it[AttachmentTable.name] = dto.name
                it[AttachmentTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[AttachmentTable.customFields] = dto.customFields.inner
                it[AttachmentTable.created] = existingItem?.created ?: now
                it[AttachmentTable.lastUpdated] = now
                it[AttachmentTable.sourcePath] = dto.sourcePath
                it[AttachmentTable.targetFolder] = dto.targetFolder
                it[AttachmentTable.attachmentType] = dto.attachmentType.name
                it[AttachmentTable.skip] = dto.skip
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<Attachment>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "source_path", "target_folder", "attachment_type", "skip"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)

                if (existingItem == null) {
                    statusTrackingRepository.active(dto.id, ResourceType.Attachment)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@AttachmentRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setString(index++, dto.sourcePath)
                stmt.setString(index++, dto.targetFolder)
                stmt.setString(index++, dto.attachmentType.name)
                stmt.setObject(index++, Json.encodeToString(dto.skip), Types.OTHER)
            }

            stmt.executeUpdate()
        }
    }
}
