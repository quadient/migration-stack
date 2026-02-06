package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.FileBuilder
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.FileType
import com.quadient.migration.tools.aFileRepository
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class FileRepositoryTest {
    private val projectName = aProjectConfig().name
    private val repo = aFileRepository()
    private val statusRepo = StatusTrackingRepository(projectName)

    @Test
    fun roundtrip() {
        val dto = FileBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .sourcePath("path/to/file.pdf")
            .fileType(FileType.Attachment)
            .skip("reason", "placeholder")
            .build()

        repo.upsert(dto)
        val result = repo.listAll().first()
        dto.created = result.created
        dto.lastUpdated = result.lastUpdated

        result.shouldBeEqualTo(dto)
    }

    @Test
    fun `upsert tracks active status for new objects and does not insert it again for existing objects`() {
        val dto = FileBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .fileType(FileType.Document)
            .build()

        repo.upsert(dto)
        repo.upsert(dto)

        val result = statusRepo.find(dto.id, ResourceType.File)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
    }
}
