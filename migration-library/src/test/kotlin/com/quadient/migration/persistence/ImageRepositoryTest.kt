package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.model.aImageInternalRepository
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

@Postgres
class ImageRepositoryTest {
    private val internalRepo = aImageInternalRepository()
    private val repo = ImageRepository(internalRepo)

    @Test
    fun roundtrip() {
        val dto = ImageBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .targetFolder("someFolder")
            .imageType(ImageType.Gif)
            .build()

        repo.upsert(dto)
        val result = repo.listAll()

        result.first().shouldBeEqualTo(dto)
    }
}