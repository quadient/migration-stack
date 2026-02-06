package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.*
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class TextStyleRepositoryTest {
    private val repo = aTextStyleRepository()
    private val docRepo = aDocumentObjectRepository()
    private val statusRepo = StatusTrackingRepository(aProjectConfig().name)

    @Test
    fun `roundtrip is correct`() {
        val dto = TextStyleBuilder("id").definition {
            foregroundColor("#00FF00")
            size(10.0.millimeters())
            strikethrough(true)
            bold(true)
            italic(true)
            underline(true)
            fontFamily("tahoma")
            superOrSubscript(SuperOrSubscript.Superscript)
            interspacing(69.0.millimeters())
        }.build()

        repo.upsert(dto)
        val result = repo.listAll().first()
        dto.created = result.created
        dto.lastUpdated = result.lastUpdated

        result.shouldBeEqualTo(dto)
    }

    @Test
    fun `findUsedBy`() {
        docRepo.upsert(
            aBlockDto(
                "parablock", content = listOf(
                    aParagraph(
                        content = listOf(aText(styleRef = TextStyleRef("subject1")))
                    )
                )
            )
        )
        docRepo.upsert(
            aBlockDto(
                "unused", content = listOf(
                    aParagraph(
                        content = listOf(aText(styleRef = TextStyleRef("unusedref")))
                    )
                )
            )
        )

        val result = repo.findUsages("subject1")

        result.shouldBeOfSize(1)
        result.first().id.shouldBeEqualTo("parablock")
    }

    @Test
    fun `upsert tracks active status for new objects and does not insert it again for existing objects`() {
        val dto = TextStyleBuilder("someid").definition {
            foregroundColor("#00FF00")
        }.build()

        repo.upsert(dto)
        repo.upsert(dto)

        val result = statusRepo.find(dto.id, ResourceType.TextStyle)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
    }

    @Test
    fun `upsertBatch roundtrip`() {
        // given
        val style1 = TextStyleBuilder("style1")
            .name("Text Style 1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .definition {
                foregroundColor("#FF0000")
                size(12.0.millimeters())
                bold(true)
                italic(false)
                fontFamily("Arial")
            }
            .build()

        val style2 = TextStyleBuilder("style2")
            .name("Text Style 2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .definition {
                foregroundColor("#0000FF")
                size(14.0.millimeters())
                bold(false)
                italic(true)
                fontFamily("Times New Roman")
                underline(true)
            }
            .build()

        // when
        repo.upsertBatch(listOf(style1, style2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultStyle1 = result.first { it.id == "style1" }
        val resultStyle2 = result.first { it.id == "style2" }

        resultStyle1.shouldBeEqualTo(style1)
        resultStyle2.shouldBeEqualTo(style2)

        // Update the already existing styles and assert upsert again
        resultStyle1.name = "Updated Style 1"
        resultStyle2.name = "Updated Style 2"

        repo.upsertBatch(listOf(resultStyle1, resultStyle2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedStyle1 = updatedResult.first { it.id == "style1" }
        val updatedStyle2 = updatedResult.first { it.id == "style2" }

        updatedStyle1.name.shouldBeEqualTo("Updated Style 1")
        updatedStyle2.name.shouldBeEqualTo("Updated Style 2")
    }
}