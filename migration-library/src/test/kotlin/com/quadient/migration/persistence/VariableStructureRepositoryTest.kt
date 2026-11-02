package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectName
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.tools.aVariableStructureRepository
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Test

@Postgres
class VariableStructureRepositoryTest {
    private val repo = aVariableStructureRepository()
    private val statusRepo = StatusTrackingRepository(ProjectName(aProjectConfig().name))

    @Test
    fun `upsert creates active status only for new variable structure`() {
        val dto = VariableStructureBuilder("vs-status").addVariable("var1", "Data.Test.Value").build()

        repo.upsert(dto)
        repo.upsert(dto)

        val events = statusRepo.findEventsRelevantToOutput("vs-status", ResourceType.VariableStructure, InspireOutput.Interactive)
        events.shouldBeOfSize(1)
        (events.first() is Active).shouldBeEqualTo(true)
    }

    @Test
    fun `upsertBatch creates active status only for new variable structures`() {
        val struct1 = VariableStructureBuilder("vs-batch1").addVariable("var1", "Data.Test.Value").build()
        val struct2 = VariableStructureBuilder("vs-batch2").addVariable("var2", "Data.Test.Value").build()

        repo.upsertBatch(listOf(struct1, struct2))
        repo.upsertBatch(listOf(struct1, struct2))

        for (id in listOf("vs-batch1", "vs-batch2")) {
            val events = statusRepo.findEventsRelevantToOutput(id, ResourceType.VariableStructure, InspireOutput.Interactive)
            events.shouldBeOfSize(1)
            (events.first() is Active).shouldBeEqualTo(true)
        }
    }

    @Test
    fun `roundtrip is correct`() {
        val dto = VariableStructureBuilder("id").customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2")).targetFolder("folder").addVariable("var1", "Data.Test.Value")
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
            .targetFolder("folder1")
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

        struct1.created = resultStruct1.created
        struct1.lastUpdated = resultStruct1.lastUpdated
        struct2.created = resultStruct2.created
        struct2.lastUpdated = resultStruct2.lastUpdated

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