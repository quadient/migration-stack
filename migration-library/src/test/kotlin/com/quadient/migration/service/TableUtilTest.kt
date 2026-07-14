package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.builder.FirstMatchBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TableUtilTest {
    private val mixedContent: List<DocumentContent> = listOf(
        TableBuilder().build(),
        AreaBuilder().firstMatch { defaultTable { } }.build(),
        AreaBuilder().table { }.build(),
        FirstMatchBuilder().defaultTable { }.build(),
    )

    @Test
    fun `collectDocumentTables collects direct table, one level nested table, but skips table nested two levels deep`() {
        val result = collectDocumentTables(mixedContent)

        assertEquals(3, result.size)
        result[0].contentPath.shouldBeEqualTo("table:0")
        result[1].contentPath.shouldBeEqualTo("area:1/table:0")
        result[2].contentPath.shouldBeEqualTo("firstMatch:0/table:0")
    }
}
