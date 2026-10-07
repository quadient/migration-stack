package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.shared.ColumnApplyTo
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.xmlbeans.XmlObject

import static com.quadient.migration.example.docx.parser.DocxBlocks.blockName
import static com.quadient.migration.example.docx.parser.DocxBlocks.upsertBlock
import static com.quadient.migration.example.docx.parser.DocxTableParser.addRows
import static com.quadient.migration.example.docx.parser.DocxTableParser.createTableBuilder
import static com.quadient.migration.example.docx.parser.DocxTableParser.haveSameGrid
import static com.quadient.migration.example.docx.parser.DocxTableParser.resolveExpectedColumnCount
import static com.quadient.migration.example.docx.util.DocxUtils.extractParagraphText

/**
 * Collects the body content of one page. The most recent table is kept open (unbuilt) so that a following table wrapped
 * in an IF field can be appended to it as conditional rows, which is how Word templates model optional table rows.
 * A conditional table that does not fit the preceding one is wrapped in an internal block carrying the display rule.
 * Headings are remembered so that the finished page can be divided into one block per top-level section.
 */
class DocxBodyContent {
    private final Migration migration
    private final String fileName
    private final String pageId
    private final List<DocumentContent> content = []
    // Heading level of the content items that are headings, keyed by item identity (model items are value objects).
    private final Map<DocumentContent, Integer> headingLevels = new IdentityHashMap<>()

    private TableBuilder openTable
    private XWPFTable openTableSource
    private int openTableColumnCount
    private int conditionalTableBlocks = 0

    DocxBodyContent(Migration migration, String fileName, String pageId) {
        this.migration = migration
        this.fileName = fileName
        this.pageId = pageId
    }

    void add(DocumentContent item, Integer headingLevel = null) {
        closeTable()
        content.add(item)
        if (headingLevel != null) {
            headingLevels.put(item, headingLevel)
        }
    }

    void addTable(XWPFTable table, TableBuilder tableBuilder, int columnCount) {
        closeTable()
        openTable = tableBuilder
        openTableSource = table
        openTableColumnCount = columnCount
    }

    // Handler for tables wrapped in an IF field. Its rows join a directly preceding regular table with the same grid
    // (optional rows of one table); otherwise the whole table is conditional, so it is wrapped in an internal block
    // referenced with the display rule, which keeps the table itself free of per-row rules.
    void addConditionalTable(XWPFTable table, String displayRuleId) {
        int columnCount = resolveExpectedColumnCount(table)
        if (openTable != null && openTableColumnCount == columnCount && haveSameGrid(openTableSource, table)) {
            addRows(migration, openTable, table, table.rows, fileName, columnCount, false, displayRuleId)
            return
        }
        TableBuilder tableBuilder = createTableBuilder(migration, table, true)
        addRows(migration, tableBuilder, table, table.rows, fileName, columnCount, true)
        String id = "${pageId}_table${++conditionalTableBlocks}"
        add(upsertBlock(migration, id, blockName(firstCellText(table), "table", id), [tableBuilder.build()], fileName, displayRuleId))
    }

    void closeTable() {
        if (openTable != null) {
            content.add(openTable.build())
            openTable = null
            openTableSource = null
        }
    }

    // The collected content as is, without dividing it into sections.
    List<DocumentContent> flow() {
        closeTable()
        return content
    }

    /**
     * Divides the page content into internal blocks, one per top-level heading (the lowest heading level found on the
     * page); content before the first heading forms a block of its own. Pages without headings stay a plain flow.
     */
    List<DocumentContent> sectionBlocks() {
        closeTable()
        Integer topLevel = headingLevels.values().min()
        if (topLevel == null) {
            return content
        }
        List<DocumentContent> result = []
        List<DocumentContent> section = []
        int blockNumber = 0
        ColumnLayout pendingColumnLayout
        boolean pendingColumnLayoutSpansMultipleBlocks = false
        Closure flushSection = {
            if (section.isEmpty()) {
                return
            }
            String id = "${pageId}_section${++blockNumber}"
            String heading = headingLevels[section[0]] == topLevel ? extractParagraphText(section[0]) : null
            // Put the marker in the first generated block.  A WholeTemplate marker still covers the following block
            // references, but is now followed by real block content rather than ending an otherwise separate flow.
            List<DocumentContent> blockContent = pendingColumnLayout == null ? section : [pendingColumnLayoutSpansMultipleBlocks ?
                    pendingColumnLayout : new ColumnLayout(pendingColumnLayout.numberOfColumns, pendingColumnLayout.gutterWidth,
                    pendingColumnLayout.balancingType, ColumnApplyTo.ThisBlockOnly)] + section
            pendingColumnLayout = null
            pendingColumnLayoutSpansMultipleBlocks = false
            result.add(upsertBlock(migration, id, blockName(heading, null, id), blockContent, fileName))
            section.clear()
        }
        content.eachWithIndex { DocumentContent item, int itemIndex ->
            // A marker before generated blocks retains WholeTemplate only when its Word section covers multiple
            // heading-derived blocks; otherwise it is narrowed to the first generated block.
            if (item instanceof ColumnLayout) {
                flushSection()
                pendingColumnLayout = item
                pendingColumnLayoutSpansMultipleBlocks = spansMultipleBlocks(itemIndex, topLevel)
            } else {
                if (headingLevels[item] == topLevel && !section.isEmpty()) {
                    flushSection()
                }
                section << item
            }
        }
        flushSection()
        if (pendingColumnLayout != null) {
            result.add(pendingColumnLayout)
        }
        return result
    }

    // A Word column section spanning multiple heading-derived blocks retains WholeTemplate. A one-block section is
    // deliberately kept self-contained, which is needed for local layouts such as signature blocks.
    private boolean spansMultipleBlocks(int layoutIndex, int topLevel) {
        boolean hasContent = false
        int blockCount = 0
        for (int i = layoutIndex + 1; i < content.size(); i++) {
            DocumentContent item = content[i]
            if (item instanceof ColumnLayout) {
                break
            }
            if (headingLevels[item] == topLevel && hasContent) {
                blockCount++
            }
            hasContent = true
        }
        return blockCount + (hasContent ? 1 : 0) > 1
    }

    // Text of the first non-empty cell of the table's first row (typically the heading of the row). Word stores the
    // text of a table wrapped in a field as instrText, which POI's text accessors skip.
    private static String firstCellText(XWPFTable table) {
        return table.rows[0].tableCells.collect { cellText(it).trim() }.find { it }
    }

    private static String cellText(XWPFTableCell cell) {
        return cell.paragraphs.collect { paragraph ->
            paragraph.runs.collect { run ->
                List<XmlObject> fieldChildren = DocxMergeFields.fieldChildren(run)
                fieldChildren.isEmpty() ? run.text() : DocxMergeFields.instructionText(fieldChildren)
            }.join("")
        }.join(" ")
    }
}
