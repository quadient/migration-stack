package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.tools.aVariableStructureRepository
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Test

@Postgres
class VariableStructureRepositoryTest {
    private val repo = aVariableStructureRepository()

    @Test
    fun `roundtrip is correct`() {
        val dto = VariableStructureBuilder("id").customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2")).addVariable("var1", "Data.Test.Value")
            .addVariable("var2", "Data.Test.Value2").build()

        repo.upsert(dto)
        val result = repo.listAll().first()
        dto.created = result.created
        dto.lastUpdated = result.lastUpdated

        result.shouldBeEqualTo(dto)
        result.structure.size.shouldBeEqualTo(2)
    }

    @Test
    fun `upsertBatch roundtrip`() {
        // given
        val struct1 = VariableStructureBuilder("struct1")
            .name("Variable Structure 1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .addVariable("var1", "Data.Test.Value1")
            .addVariable("var2", "Data.Test.Value2")
            .build()

        val struct2 = VariableStructureBuilder("struct2")
            .name("Variable Structure 2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .addVariable("var3", "Data.Test.Value3")
            .addVariable("var4", "Data.Test.Value4")
            .addVariable("var5", "Data.Test.Value5")
            .build()

        // when
        repo.upsertBatch(listOf(struct1, struct2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultStruct1 = result.first { it.id == "struct1" }
        val resultStruct2 = result.first { it.id == "struct2" }

        resultStruct1.shouldBeEqualTo(struct1)
        resultStruct2.shouldBeEqualTo(struct2)
        resultStruct1.structure.size.shouldBeEqualTo(2)
        resultStruct2.structure.size.shouldBeEqualTo(3)

        // Update the already existing structures and assert upsert again
        resultStruct1.name = "Updated Structure 1"
        resultStruct2.name = "Updated Structure 2"

        repo.upsertBatch(listOf(resultStruct1, resultStruct2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedStruct1 = updatedResult.first { it.id == "struct1" }
        val updatedStruct2 = updatedResult.first { it.id == "struct2" }

        updatedStruct1.name.shouldBeEqualTo("Updated Structure 1")
        updatedStruct2.name.shouldBeEqualTo("Updated Structure 2")
    }
}