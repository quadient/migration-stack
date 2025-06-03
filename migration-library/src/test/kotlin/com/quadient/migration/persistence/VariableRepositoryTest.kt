package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.shared.DataType
import com.quadient.migration.tools.*
import org.junit.jupiter.api.Test

@Postgres
class VariableRepositoryTest {
    private val documentRepo = aDocumentObjectRepository()
    private val variableRepo = aVariableRepository()

    @Test
    fun `roundtrip is correct`() {
        val dto = VariableBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .dataType(DataType.Currency)
            .defaultValue("default value")
            .build()

        variableRepo.upsert(dto)
        val result = variableRepo.listAll()

        result.first().shouldBeEqualTo(dto)
    }

    @Test
    fun `reports usedBy correctly`() {
        documentRepo.upsert(aBlockDto("parent1", listOf(DocumentObjectRef("subjectBlock"))))
        documentRepo.upsert(aBlockDto("parent2", listOf(DocumentObjectRef("subjectBlock"))))
        documentRepo.upsert(aBlockDto("parent3", listOf(DocumentObjectRef("subjectBlock"))))
        documentRepo.upsert(aBlockDto("notParent1", listOf(DocumentObjectRef("nonSubjectBlock"))))
        documentRepo.upsert(aBlockDto("notParent2", listOf(DocumentObjectRef("nonSubjectBlock"))))
        variableRepo.upsert(aVariable("subjectVariable"))

        val result = variableRepo.findUsages("subjectBlock")

        result.shouldBeOfSize(3)
        result.map { it.id }.shouldBeEqualTo(listOf("parent1", "parent2", "parent3"))
    }

    @Test
    fun `findUsedBy`() {
        documentRepo.upsert(
            aBlockDto(
                "parablock", content = listOf(
                    aParagraph(
                        content = listOf(aText(content = listOf(VariableRef("subject1"))))
                    )
                )
            )
        )
        documentRepo.upsert(
            aBlockDto(
                "unused", content = listOf(
                    aParagraph(
                        content = listOf(aText(content = listOf(VariableRef("unused"))))
                    )
                )
            )
        )

        val result = variableRepo.findUsages("subject1")

        result.shouldBeOfSize(1)
        result.first().id.shouldBeEqualTo("parablock")
    }
}