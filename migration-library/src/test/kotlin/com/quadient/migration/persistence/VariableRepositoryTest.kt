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
        val result = variableRepo.listAll().first()
        dto.created = result.created
        dto.lastUpdated = result.lastUpdated

        result.shouldBeEqualTo(dto)
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

    @Test
    fun `upsertBatch roundtrip`() {
        // given
        val var1 = VariableBuilder("var1")
            .name("Variable 1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .dataType(DataType.String)
            .defaultValue("default1")
            .build()

        val var2 = VariableBuilder("var2")
            .name("Variable 2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .dataType(DataType.Currency)
            .defaultValue("default2")
            .build()

        // when
        variableRepo.upsertBatch(listOf(var1, var2))

        // then
        val result = variableRepo.listAll()
        result.shouldBeOfSize(2)

        val resultVar1 = result.first { it.id == "var1" }
        val resultVar2 = result.first { it.id == "var2" }

        var1.created = resultVar1.created
        var1.lastUpdated = resultVar1.lastUpdated
        var2.created = resultVar2.created
        var2.lastUpdated = resultVar2.lastUpdated

        resultVar1.shouldBeEqualTo(var1)
        resultVar2.shouldBeEqualTo(var2)

        // Update the already existing variables and assert upsert again
        resultVar1.name = "Updated Variable 1"
        resultVar2.name = "Updated Variable 2"

        variableRepo.upsertBatch(listOf(resultVar1, resultVar2))

        val updatedResult = variableRepo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedVar1 = updatedResult.first { it.id == "var1" }
        val updatedVar2 = updatedResult.first { it.id == "var2" }

        updatedVar1.name.shouldBeEqualTo("Updated Variable 1")
        updatedVar2.name.shouldBeEqualTo("Updated Variable 2")
    }
}