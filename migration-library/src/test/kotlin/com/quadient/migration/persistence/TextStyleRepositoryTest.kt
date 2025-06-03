package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.*
import org.junit.jupiter.api.Test

@Postgres
class TextStyleRepositoryTest {
    private val repo = aTextStyleRepository()
    private val docRepo = aDocumentObjectRepository()

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
        val result = repo.listAll()

        result.first().shouldBeEqualTo(dto)
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
}