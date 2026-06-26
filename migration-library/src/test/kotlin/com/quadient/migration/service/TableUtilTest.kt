package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.builder.FirstMatchBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.shared.VariableRefPath
import com.quadient.migration.shared.millimeters
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

    private val richTable = TableBuilder().addColumnWidth(10.millimeters(), 50.0).addColumnWidth(10.millimeters(), 50.0)
        .addRepeatedRow(VariableRefPath("itemVar")) {
            addRow {
                addCell { string("Item A") }
                addCell { string("Item B") }
            }
        }.addFirstHeaderRow {
            addCell { string("Alpha") }
            addCell { string("Beta") }
        }.build()

    @Test
    fun `collectDocumentTables collects direct table, area-child table and FM-child table, but skips table nested inside FM inside area`() {
        val result = collectDocumentTables(mixedContent)

        assertEquals(3, result.size)
        result[0].contentPath.shouldBeEqualTo("table:0")
        result[1].contentPath.shouldBeEqualTo("area:1/table:0")
        result[2].contentPath.shouldBeEqualTo("firstMatch:0/table:0")
    }

    @Test
    fun `computeFingerprint encodes column count, repeated variable and first header row preview`() {
        computeFingerprint(richTable).shouldBeEqualTo("2cols|repeatedBy:itemVar|Alpha|Beta")
    }

    @Test
    fun `buildContentPreview formats column count, repeated variable label and first header row label with preview`() {
        buildContentPreview(richTable).shouldBeEqualTo("2 cols | repeatedBy: itemVar | firstHeader: Alpha | Beta")
    }
}
