package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Tab
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.TabType
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.aBlockDto
import com.quadient.migration.tools.aDocumentObjectRepository
import com.quadient.migration.tools.aParaStyleRepository
import com.quadient.migration.tools.aParagraph
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class ParagraphStyleRepositoryTest {
    private val repo = aParaStyleRepository()
    private val docRepo = aDocumentObjectRepository()
    private val statusRepo = StatusTrackingRepository(aProjectConfig().name)

    @Test
    fun `roundtrip is correct`() {
        val dto = ParagraphStyleBuilder("id").definition {
            defaultTabSize(10.2.millimeters())
            leftIndent(3.2.millimeters())
            rightIndent(3.8.millimeters())
            spaceAfter(8.8.millimeters())
            spaceBefore(9.9.millimeters())
            alignment(Alignment.JustifyRight)
            firstLineIndent(1.1.millimeters())
            lineSpacing(LineSpacing.ExactFromPrevious(333.millimeters()))
            keepWithNextParagraph(true)
            tabs(
                Tabs(
                    tabs = listOf(
                        Tab(position = 88.9.millimeters(), type = TabType.DecimalWord)
                    ), useOutsideTabs = true
                )
            )
        }.build()

        repo.upsert(dto)
        val result = repo.listAll()

        result.first().shouldBeEqualTo(dto)
    }

    @Test
    fun `findUsedBy`() {
        docRepo.upsert(aBlockDto("parablock", content = listOf(aParagraph(styleRef = ParagraphStyleRef("subject1")))))
        docRepo.upsert(aBlockDto("unused", content = listOf(aParagraph(styleRef = ParagraphStyleRef("unusedref")))))

        val result = repo.findUsages("subject1")

        result.shouldBeOfSize(1)
        result.first().id.shouldBeEqualTo("parablock")
    }

    @Test
    fun `upsert tracks active status for new objects and does not insert it again for existing objects`() {
        val dto = ParagraphStyleBuilder("id").definition {
            defaultTabSize(10.2.millimeters())
        }.build()

        repo.upsert(dto)
        repo.upsert(dto)

        val result = statusRepo.find(dto.id, ResourceType.ParagraphStyle)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
    }

    @Test
    fun `upsertBatch roundtrip`() {
        // given
        val style1 = ParagraphStyleBuilder("style1")
            .name("Paragraph Style 1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .definition {
                defaultTabSize(10.0.millimeters())
                leftIndent(5.0.millimeters())
                alignment(Alignment.JustifyLeft)
                lineSpacing(LineSpacing.ExactFromPrevious(12.millimeters()))
            }
            .build()

        val style2 = ParagraphStyleBuilder("style2")
            .name("Paragraph Style 2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .definition {
                defaultTabSize(15.0.millimeters())
                rightIndent(8.0.millimeters())
                alignment(Alignment.JustifyRight)
                keepWithNextParagraph(true)
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