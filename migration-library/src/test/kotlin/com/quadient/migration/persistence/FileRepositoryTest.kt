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
    fun `upsertBatch roundtrip`() {
        // given
        val file1 = FileBuilder("file1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .targetFolder("folder1")
            .sourcePath("path/to/file1.pdf")
            .fileType(FileType.Attachment)
            .skip("reason1", "placeholder1")
            .build()

        val file2 = FileBuilder("file2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .targetFolder("folder2")
            .sourcePath("path/to/file2.doc")
            .fileType(FileType.Document)
            .skip("reason2", "placeholder2")
            .build()

        // when
        repo.upsertBatch(listOf(file1, file2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultFile1 = result.first { it.id == "file1" }
        val resultFile2 = result.first { it.id == "file2" }

        resultFile1.shouldBeEqualTo(file1)
        resultFile2.shouldBeEqualTo(file2)

        // Update the already existing files and assert upsert again
        resultFile1.targetFolder = "updatedFolder1"
        resultFile2.targetFolder = "updatedFolder2"

        repo.upsertBatch(listOf(resultFile1, resultFile2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedFile1 = updatedResult.first { it.id == "file1" }
        val updatedFile2 = updatedResult.first { it.id == "file2" }

        updatedFile1.targetFolder.shouldBeEqualTo("updatedFolder1")
        updatedFile2.targetFolder.shouldBeEqualTo("updatedFolder2")
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
