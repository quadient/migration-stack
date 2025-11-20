package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aImageInternalRepository
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class ImageRepositoryTest {
    private val internalRepo = aImageInternalRepository()
    private val repo = ImageRepository(internalRepo)
    private val statusRepo = StatusTrackingRepository(internalRepo.projectName)

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
            .build()

        repo.upsert(dto)
        val result = repo.listAll()

        result.first().shouldBeEqualTo(dto)
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
}