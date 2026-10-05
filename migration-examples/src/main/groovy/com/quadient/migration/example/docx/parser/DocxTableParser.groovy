package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule
import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseFlowParagraph
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseParagraph
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.warnUnterminatedField
import static com.quadient.migration.example.docx.style.DocxTableStyling.applyCellStyling
import static com.quadient.migration.example.docx.style.DocxTableStyling.applyColumnWidths
import static com.quadient.migration.example.docx.style.DocxTableStyling.applyRowHeight
import static com.quadient.migration.example.docx.util.DocxUtils.isHorizontallyMergedCell

@Field
static final long GRID_TOLERANCE_TWIPS = 30

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

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
            log.warn "  Warning: Row ${ri + 1} has ${row.tableCells.size()} cells, expected ${expectedColumnCount}."
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
            log.warn "  Adding ${missingCells} empty cells to row ${ri + 1} to match expected column count."
            missingCells.times { rowBuilder.addCell().mergeLeft = true }
        }
    }
}

// Paragraphs of one cell share the field state so an IF whose instruction and result sit in different paragraphs resolves.
private static List<DocumentContent> parseCellParagraphs(Migration migration, XWPFTableCell cell, String fileName, String context) {
    FieldParseState fieldState = new FieldParseState()
    List<Map> parsedParagraphs = cell.paragraphs.findResults { XWPFParagraph paragraph ->
        Paragraph parsed = parseFlowParagraph(migration, paragraph, fileName, fieldState, context)
        parsed == null ? null : [source: paragraph, parsed: parsed]
    }
    warnUnterminatedField(fieldState, "table cell: '${cell.text}'")
    List<DocumentContent> paragraphs = mergeVmlImageAnchorParagraphs(parsedParagraphs)
    if (paragraphs.isEmpty() && cell.paragraphs) {
        paragraphs.add(parseParagraph(migration, cell.paragraphs.first(), fileName, context))
    }
    return paragraphs
}

private static List<DocumentContent> mergeVmlImageAnchorParagraphs(List<Map> paragraphs) {
    List<DocumentContent> result = []
    for (int i = 0; i < paragraphs.size(); i++) {
        Map current = paragraphs[i]
        Paragraph images = isVmlImageAnchorParagraph(current.source as XWPFParagraph)
                ? keepOnlyImages(current.parsed as Paragraph)
                : null
        Map next = i + 1 < paragraphs.size() ? paragraphs[i + 1] : null
        if (images != null && next != null && !isVmlImageAnchorParagraph(next.source as XWPFParagraph)
                && hasVisibleContent(next.parsed as Paragraph)) {
            result.add(prependImagesWithNaturalSpacing(images, next.parsed as Paragraph))
            i++
        } else {
            result.add(current.parsed as Paragraph)
        }
    }
    return result
}

private static boolean isVmlImageAnchorParagraph(XWPFParagraph paragraph) {
    return DocxVmlImages.hasAbsolutelyPositionedImage(paragraph) && !paragraph.text?.trim()
}

private static Paragraph keepOnlyImages(Paragraph paragraph) {
    List<Paragraph.Text> images = paragraph.content.findResults { Paragraph.Text text ->
        def imageRefs = text.content.findAll { it instanceof ImageRef }
        imageRefs ? new Paragraph.Text(imageRefs, text.styleRef, text.displayRuleRef) : null
    }
    return images.isEmpty() ? null : new Paragraph(images, paragraph.styleRef, paragraph.displayRuleRef)
}

private static boolean hasVisibleContent(Paragraph paragraph) {
    return paragraph.content.any { Paragraph.Text text ->
        text.content.any { !(it instanceof StringValue) || it.value?.trim() }
    }
}

private static Paragraph prependImagesWithNaturalSpacing(Paragraph images, Paragraph paragraph) {
    return new Paragraph(images.content + replaceLeadingWhitespaceWithSingleSpace(paragraph.content),
            paragraph.styleRef, paragraph.displayRuleRef)
}

private static List<Paragraph.Text> replaceLeadingWhitespaceWithSingleSpace(List<Paragraph.Text> texts) {
    boolean leading = true
    return texts.findResults { Paragraph.Text text ->
        List trimmedContent = []
        text.content.each { item ->
            if (leading && item instanceof StringValue) {
                String value = item.value.replaceFirst(/^\s+/, '')
                if (value) {
                    trimmedContent.add(new StringValue(" ${value}"))
                    leading = false
                }
            } else {
                if (leading) {
                    trimmedContent.add(new StringValue(" "))
                }
                trimmedContent.add(item)
                leading = false
            }
        }
        trimmedContent.isEmpty() ? null : new Paragraph.Text(trimmedContent, text.styleRef, text.displayRuleRef)
    }
}
