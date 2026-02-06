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
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

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
                it[FileTable.projectName] = projectName
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
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.File)
            }

            this[FileTable.id] = dto.id
            this[FileTable.projectName] = projectName
            this[FileTable.name] = dto.name
            this[FileTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[FileTable.customFields] = dto.customFields.inner
            this[FileTable.created] = existingItem?.created ?: now
            this[FileTable.lastUpdated] = now
            this[FileTable.sourcePath] = dto.sourcePath
            this[FileTable.targetFolder] = dto.targetFolder
            this[FileTable.fileType] = dto.fileType.name
            this[FileTable.skip] = dto.skip
        }
    }
}
