package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule
import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow

import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseFlowParagraph
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseParagraph
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.warnUnterminatedField
import static com.quadient.migration.example.docx.style.DocxTableStyling.applyCellStyling
import static com.quadient.migration.example.docx.style.DocxTableStyling.applyColumnWidths
import static com.quadient.migration.example.docx.style.DocxTableStyling.applyRowHeight
import static com.quadient.migration.example.docx.util.DocxUtils.isHorizontallyMergedCell

@Field
static final long GRID_TOLERANCE_TWIPS = 30

static Table parseTable(Migration migration, XWPFTable table, String fileName) {
    return buildTable(migration, table, table.rows, fileName, resolveExpectedColumnCount(table), true)
}

static int resolveExpectedColumnCount(XWPFTable table) {
    def gridCols = table.CTTbl.tblGrid?.gridColList
    int gridColumnCount = gridCols ? gridCols.size() : table.rows[0].tableCells.size()
    int maxCellsInRow = table.rows.collect { it.tableCells.size() }.max()
    return Math.max(gridColumnCount, maxCellsInRow)
}

static Table buildTable(Migration migration, XWPFTable sourceTable, List<XWPFTableRow> rows, String fileName,
                        int expectedColumnCount, boolean useHeader) {
    TableBuilder tableBuilder = createTableBuilder(migration, sourceTable, useHeader)
    addRows(migration, tableBuilder, sourceTable, rows, fileName, expectedColumnCount, useHeader)
    return tableBuilder.build()
}

static TableBuilder createTableBuilder(Migration migration, XWPFTable sourceTable, boolean useHeader) {
    TableBuilder tableBuilder = new TableBuilder()
            .pdfTaggingRule(useHeader ? TablePdfTaggingRule.Table : TablePdfTaggingRule.None)
            .alignment(TableAlignment.Center)
    applyColumnWidths(tableBuilder, sourceTable)
    if (migration.projectConfig.context.defaultTableStyleName) {
        tableBuilder.tableStyleName(migration.projectConfig.context.defaultTableStyleName.toString())
    }
    return tableBuilder
}

// Two Word tables can be joined into one model table when their column grids match. Word lets grid widths drift by a
// few twips between otherwise identical tables, so a small tolerance is applied.
static boolean haveSameGrid(XWPFTable first, XWPFTable second) {
    List<Long> firstWidths = first.CTTbl.tblGrid?.gridColList?.collect { it.w as Long } ?: []
    List<Long> secondWidths = second.CTTbl.tblGrid?.gridColList?.collect { it.w as Long } ?: []
    return firstWidths.size() == secondWidths.size()
            && [firstWidths, secondWidths].transpose().every { Math.abs((it[0] as long) - (it[1] as long)) <= GRID_TOLERANCE_TWIPS }
}

static void addRows(Migration migration, TableBuilder tableBuilder, XWPFTable sourceTable, List<XWPFTableRow> rows, String fileName,
                    int expectedColumnCount, boolean useHeader, String displayRuleId = null) {
    rows.eachWithIndex { XWPFTableRow row, int ri ->
        if (row.tableCells.size() != expectedColumnCount) {
            println "  Warning: Row ${ri + 1} has ${row.tableCells.size()} cells, expected ${expectedColumnCount}."
        }
        boolean isHeader = useHeader && ri == 0 && rows.size() > 1
        TableBuilder.Row rowBuilder = isHeader ? tableBuilder.addFirstHeaderRow() : tableBuilder.addRow()
        if (displayRuleId) {
            rowBuilder.displayRuleRef(displayRuleId)
        }
        String context = isHeader ? "tableHeader" : "tableRow"

        row.tableCells.eachWithIndex { XWPFTableCell cell, int ci ->
            TableBuilder.Cell cellBuilder = rowBuilder.addCell()
            applyCellStyling(cellBuilder, cell, sourceTable, ri, ci, rows.size(), row.tableCells.size())
            applyRowHeight(cellBuilder, row)
            if (isHorizontallyMergedCell(cell)) {
                cellBuilder.mergeLeft = true
            } else {
                cellBuilder.content(parseCellParagraphs(migration, cell, fileName, context))
            }
        }
        int missingCells = expectedColumnCount - rowBuilder.cells.size()
        if (missingCells > 0) {
            println "  Adding ${missingCells} empty cells to row ${ri + 1} to match expected column count."
            missingCells.times { rowBuilder.addCell().mergeLeft = true }
        }
    }
}

// Paragraphs of one cell share the field state so an IF whose instruction and result sit in different paragraphs resolves.
private static List<DocumentContent> parseCellParagraphs(Migration migration, XWPFTableCell cell, String fileName, String context) {
    FieldParseState fieldState = new FieldParseState()
    List<DocumentContent> paragraphs = cell.paragraphs.findResults { parseFlowParagraph(migration, it, fileName, fieldState, context) }
    warnUnterminatedField(fieldState, "table cell: '${cell.text}'")
    if (paragraphs.isEmpty() && cell.paragraphs) {
        paragraphs.add(parseParagraph(migration, cell.paragraphs.first(), fileName, context))
    }
    return paragraphs
}
