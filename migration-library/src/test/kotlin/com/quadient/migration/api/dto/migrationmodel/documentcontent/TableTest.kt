package com.quadient.migration.api.dto.migrationmodel.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.shared.VariableRefPath
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TableTest {
    private val richTable = TableBuilder()
        .addColumnWidth(10.millimeters(), 50.0)
        .addColumnWidth(10.millimeters(), 50.0)
        .addRepeatedRow(VariableRefPath("itemVar")) {
            addRow {
                addCell { string("Item A") }
                addCell { string("Item B") }
            }
        }
        .addFirstHeaderRow {
            addCell { string("Alpha") }
            addCell { string("Beta") }
        }
        .build()

    @Test
    fun `computeFingerprint encodes column count, row count, repeated variable and first header row preview`() {
        richTable.computeFingerprint().shouldBeEqualTo("2cols|1rows|repeatedBy:itemVar|Alpha|Beta")
    }

    @Test
    fun `toPreview formats column count, repeated variable label and first header row label with preview`() {
        richTable.toPreview().shouldBeEqualTo($$"table: 2 cols | repeatedBy: $itemVar$ | firstHeader: Alpha | Beta")
    }

    @Test
    fun `toPreview resolves repeated variable name via nameResolver`() {
        val nameResolver: (DocumentContent) -> String? = { if (it is VariableRef) "Item Variable" else null }

        richTable.toPreview(nameResolver)
            .shouldBeEqualTo($$"table: 2 cols | repeatedBy: $Item Variable$ | firstHeader: Alpha | Beta")
    }

    @Test
    fun `toPreview and fingerprint shows first row with mixed string and variable ref content`() {
        val table = TableBuilder()
            .addColumnWidth(10.millimeters(), 50.0)
            .addColumnWidth(10.millimeters(), 50.0)
            .addRow {
                addCell {
                    string("Hello ")
                    variableRef("name")
                }
                addCell { string("static text") }
            }
            .build()

        table.toPreview().shouldBeEqualTo($$"table: 2 cols | 1 body rows | row0: Hello $name$ | static text")
        table.computeFingerprint().shouldBeEqualTo($$"2cols|1rows|Hello $name$|static text")
    }

    @Test
    fun `toPreview and fingerprint handles imageRef, selectByLanguage and paragraph content types in cells`() {
        val table = TableBuilder()
            .addColumnWidth(10.millimeters(), 33.0)
            .addColumnWidth(10.millimeters(), 33.0)
            .addColumnWidth(10.millimeters(), 34.0)
            .addRow {
                addCell { imageRef("photo1") }
                addCell {
                    selectByLanguage {
                        case { language("en").string("Hello") }
                        case { language("de").string("Hallo") }
                    }
                }
                addCell {
                    paragraph { string("caption text") }
                }
            }
            .build()

        table.toPreview()
            .shouldBeEqualTo("table: 3 cols | 1 body rows | row0: imageRef: photo1 | selectByLanguage: 2 cases | par: caption text")
        table.computeFingerprint().shouldBeEqualTo("3cols|1rows|imageRef: photo1|selectByLanguage: 2 cases|par: caption text")
    }
}
