package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.FileTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.FileType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import java.sql.Types

class FileRepository(table: FileTable, projectName: String) : Repository<File>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)
    override fun fromDb(row: ResultRow): File {
        return File(
            id = row[FileTable.id].value,
            name = row[FileTable.name],
            originLocations = row[FileTable.originLocations],
            customFields = CustomFieldMap(row[FileTable.customFields].toMutableMap()),
            created = row[FileTable.created],
            lastUpdated = row[FileTable.created],
            sourcePath = row[FileTable.sourcePath],
            targetFolder = row[FileTable.targetFolder],
            fileType = FileType.valueOf(row[FileTable.fileType]),
            skip = row[FileTable.skip],
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: File) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.File)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[FileTable.id] = dto.id
                it[FileTable.projectName] = this@FileRepository.projectName
                it[FileTable.name] = dto.name
                it[FileTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[FileTable.customFields] = dto.customFields.inner
                it[FileTable.created] = existingItem?.created ?: now
                it[FileTable.lastUpdated] = now
                it[FileTable.sourcePath] = dto.sourcePath
                it[FileTable.targetFolder] = dto.targetFolder
                it[FileTable.fileType] = dto.fileType.name
                it[FileTable.skip] = dto.skip
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<File>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "source_path", "target_folder", "file_type", "skip"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)

                if (existingItem == null) {
                    statusTrackingRepository.active(dto.id, ResourceType.File)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@FileRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setString(index++, dto.sourcePath)
                stmt.setString(index++, dto.targetFolder)
                stmt.setString(index++, dto.fileType.name)
                stmt.setObject(index++, Json.encodeToString(dto.skip), Types.OTHER)
            }

            stmt.executeUpdate()
        }
    }
}
