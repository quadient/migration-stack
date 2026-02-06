package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.tools.aDisplayRuleRepository
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Test

@Postgres
class DisplayRuleRepositoryTest {
    private val repo = aDisplayRuleRepository()

    @Test
    fun `roundtrip is correct`() {
        val dto = DisplayRuleBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .definition(DisplayRuleDefinition(group = Group(items = emptyList(), operator = GroupOp.Or, false)))
            .build()

        repo.upsert(dto)
        val result = repo.listAll().first()
        dto.created = result.created
        dto.lastUpdated = result.lastUpdated

        result.shouldBeEqualTo(dto)
    }

    @Test
    fun `upsertBatch roundtrip`() {
        // given
        val rule1 = DisplayRuleBuilder("rule1")
            .name("Display Rule 1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .definition(DisplayRuleDefinition(group = Group(items = emptyList(), operator = GroupOp.And, false)))
            .build()

        val rule2 = DisplayRuleBuilder("rule2")
            .name("Display Rule 2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .definition(DisplayRuleDefinition(group = Group(items = emptyList(), operator = GroupOp.Or, true)))
            .build()

        // when
        repo.upsertBatch(listOf(rule1, rule2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultRule1 = result.first { it.id == "rule1" }
        val resultRule2 = result.first { it.id == "rule2" }

        rule1.created = resultRule1.created
        rule1.lastUpdated = resultRule1.lastUpdated
        rule2.created = resultRule2.created
        rule2.lastUpdated = resultRule2.lastUpdated

        resultRule1.shouldBeEqualTo(rule1)
        resultRule2.shouldBeEqualTo(rule2)

        // Update the already existing rules and assert upsert again
        resultRule1.name = "Updated Rule 1"
        resultRule2.name = "Updated Rule 2"

        repo.upsertBatch(listOf(resultRule1, resultRule2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedBlock1 = updatedResult.first { it.id == "rule1" }
        val updatedBlock2 = updatedResult.first { it.id == "rule2" }

        updatedBlock1.name.shouldBeEqualTo("Updated Rule 1")
        updatedBlock2.name.shouldBeEqualTo("Updated Rule 2")
    }
}