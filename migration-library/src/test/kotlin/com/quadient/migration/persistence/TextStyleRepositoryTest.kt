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
}