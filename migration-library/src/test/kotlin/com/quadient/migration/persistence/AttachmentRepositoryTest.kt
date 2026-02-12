package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.tools.aAttachmentRepository
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class AttachmentRepositoryTest {
    private val projectName = aProjectConfig().name
    private val repo = aAttachmentRepository()
    private val statusRepo = StatusTrackingRepository(projectName)

    @Test
    fun roundtrip() {
        val dto = AttachmentBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .sourcePath("path/to/attachment.pdf")
            .attachmentType(AttachmentType.Attachment)
            .skip("reason", "placeholder")
            .targetImageId("imageId")
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
        val attachment1 = AttachmentBuilder("attachment1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .targetFolder("folder1")
            .sourcePath("path/to/attachment1.pdf")
            .attachmentType(AttachmentType.Attachment)
            .skip("reason1", "placeholder1")
            .build()

        val attachment2 = AttachmentBuilder("attachment2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .targetFolder("folder2")
            .sourcePath("path/to/attachment2.doc")
            .attachmentType(AttachmentType.Attachment)
            .skip("reason2", "placeholder2")
            .build()

        // when
        repo.upsertBatch(listOf(attachment1, attachment2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultAttachment1 = result.first { it.id == "attachment1" }
        val resultAttachment2 = result.first { it.id == "attachment2" }

        attachment1.created = resultAttachment1.created
        attachment1.lastUpdated = resultAttachment1.lastUpdated
        attachment2.created = resultAttachment2.created
        attachment2.lastUpdated = resultAttachment2.lastUpdated

        resultAttachment1.shouldBeEqualTo(attachment1)
        resultAttachment2.shouldBeEqualTo(attachment2)

        // Update the already existing attachments and assert upsert again
        resultAttachment1.targetFolder = "updatedFolder1"
        resultAttachment2.targetFolder = "updatedFolder2"

        repo.upsertBatch(listOf(resultAttachment1, resultAttachment2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedAttachment1 = updatedResult.first { it.id == "attachment1" }
        val updatedAttachment2 = updatedResult.first { it.id == "attachment2" }

        updatedAttachment1.targetFolder.shouldBeEqualTo("updatedFolder1")
        updatedAttachment2.targetFolder.shouldBeEqualTo("updatedFolder2")
    }

    @Test
    fun `upsert tracks active status for new objects and does not insert it again for existing objects`() {
        val dto = AttachmentBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .attachmentType(AttachmentType.Attachment)
            .build()

        repo.upsert(dto)
        repo.upsert(dto)

        val result = statusRepo.find(dto.id, ResourceType.Attachment)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
    }
}
