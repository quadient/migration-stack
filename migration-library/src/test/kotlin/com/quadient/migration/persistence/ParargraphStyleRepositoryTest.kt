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
        val result = repo.listAll().first()
        dto.created = result.created
        dto.lastUpdated = result.lastUpdated

        result.shouldBeEqualTo(dto)
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
}