package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.tools.aDisplayRuleRepository
import com.quadient.migration.tools.shouldBeEqualTo
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
        val result = repo.listAll()

        result.first().shouldBeEqualTo(dto)
    }
}