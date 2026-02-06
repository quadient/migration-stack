package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.tools.aVariableStructureRepository
import com.quadient.migration.tools.shouldBeEqualTo
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
}