package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.aImageRepository
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class ImageRepositoryTest {
    private val projectName = aProjectConfig().name
    private val repo = aImageRepository()
    private val statusRepo = StatusTrackingRepository(projectName)

    @Test
    fun roundtrip() {
        val dto = ImageBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .metadata("test") {
                string("value")
                integer(123)
                boolean(true)
            }
            .imageType(ImageType.Gif)
            .skip("reason", "placeholder")
            .alternateText("some alt text")
            .targetAttachmentId("attachmentId")
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
        val image1 = ImageBuilder("image1")
            .name("Image 1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .targetFolder("folder1")
            .metadata("meta1") {
                string("data1")
            }
            .imageType(ImageType.Png)
            .skip("reason1", "placeholder1")
            .alternateText("alt text 1")
            .build()

        val image2 = ImageBuilder("image2")
            .name("Image 2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .targetFolder("folder2")
            .metadata("meta2") {
                string("data2")
            }
            .imageType(ImageType.Jpeg)
            .skip("reason2", "placeholder2")
            .alternateText("alt text 2")
            .build()

        // when
        repo.upsertBatch(listOf(image1, image2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultImage1 = result.first { it.id == "image1" }
        val resultImage2 = result.first { it.id == "image2" }

        image1.created = resultImage1.created
        image1.lastUpdated = resultImage1.lastUpdated
        image2.created = resultImage2.created
        image2.lastUpdated = resultImage2.lastUpdated

        resultImage1.shouldBeEqualTo(image1)
        resultImage2.shouldBeEqualTo(image2)

        // Update the already existing images and assert upsert again
        resultImage1.targetFolder = "updatedFolder1"
        resultImage2.targetFolder = "updatedFolder2"

        repo.upsertBatch(listOf(resultImage1, resultImage2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedImage1 = updatedResult.first { it.id == "image1" }
        val updatedImage2 = updatedResult.first { it.id == "image2" }

        updatedImage1.targetFolder.shouldBeEqualTo("updatedFolder1")
        updatedImage2.targetFolder.shouldBeEqualTo("updatedFolder2")
    }

    @Test
    fun `upsert tracks active status for new objects and does not insert it again for existing objects`() {
        val dto = ImageBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .imageType(ImageType.Gif)
            .build()

        repo.upsert(dto)
        repo.upsert(dto)

        val result = statusRepo.find(dto.id, ResourceType.Image)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
    }

    @Test
    fun `upsertBatch tracks active status for new objects and does not insert it again for existing objects`() {
        val dto1 = ImageBuilder("batch1")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1"))
            .targetFolder("someFolder")
            .imageType(ImageType.Gif)
            .build()

        val dto2 = ImageBuilder("batch2")
            .customFields(mutableMapOf("f2" to "val2"))
            .originLocations(listOf("test2"))
            .targetFolder("someFolder")
            .imageType(ImageType.Png)
            .build()

        repo.upsertBatch(listOf(dto1, dto2))
        repo.upsertBatch(listOf(dto1, dto2))

        val result1 = statusRepo.find(dto1.id, ResourceType.Image)
        val result2 = statusRepo.find(dto2.id, ResourceType.Image)

        result1?.statusEvents?.shouldBeOfSize(1)
        result2?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result1?.statusEvents?.last())
        assertInstanceOf(Active::class.java, result2?.statusEvents?.last())
    }
}