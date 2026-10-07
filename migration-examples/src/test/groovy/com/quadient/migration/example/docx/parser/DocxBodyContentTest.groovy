package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType

import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static com.quadient.migration.example.docx.DocxFieldFixtures.*
import static com.quadient.migration.example.Utils.mockMigration

class DocxBodyContentTest {

    @Test
    void "column layout before one generated section block is scoped to that block"() {
        // given: a continuous section starts between two heading-derived blocks
        def migration = mockMigration()
        def body = new DocxBodyContent(migration, 'sample', 'sample_page1')
        body.add(new ParagraphBuilder().string('Before columns').build(), 1)
        body.add(new ColumnLayout(2, Size.ofPoints(17), null, ColumnApplyTo.WholeTemplate))
        body.add(new ParagraphBuilder().string('In columns').build())

        // when
        def content = body.sectionBlocks()

        // then: the marker is moved into the following referenced block with the narrower scope
        assert content*.id == ['sample_page1_section1', 'sample_page1_section2']
        def blockCaptor = ArgumentCaptor.forClass(DocumentObject)
        verify(migration.documentObjectRepository, times(2)).upsert(blockCaptor.capture())
        DocumentObject columnBlock = blockCaptor.allValues.find { it.id == 'sample_page1_section2' }
        assert columnBlock.content[0] instanceof ColumnLayout
        assert (columnBlock.content[0] as ColumnLayout).applyTo == ColumnApplyTo.ThisBlockOnly
    }

    @Test
    void "column layout before several generated section blocks stays whole-template in the first block"() {
        // given: Word's two-column section covers several heading-derived document-object blocks
        def migration = mockMigration()
        def body = new DocxBodyContent(migration, 'sample', 'sample_page1')
        body.add(new ColumnLayout(2, Size.ofPoints(17), null, ColumnApplyTo.WholeTemplate))
        body.add(new ParagraphBuilder().string('First section').build(), 1)
        body.add(new ParagraphBuilder().string('Second section').build(), 1)

        // when
        def content = body.sectionBlocks()

        // then: it is followed by content while retaining WholeTemplate for the following generated block
        assert content*.id == ['sample_page1_section1', 'sample_page1_section2']
        def blockCaptor = ArgumentCaptor.forClass(DocumentObject)
        verify(migration.documentObjectRepository, times(2)).upsert(blockCaptor.capture())
        DocumentObject firstColumnBlock = blockCaptor.allValues.find { it.id == 'sample_page1_section1' }
        assert firstColumnBlock.content[0] instanceof ColumnLayout
        assert (firstColumnBlock.content[0] as ColumnLayout).applyTo == ColumnApplyTo.WholeTemplate
    }

    @Test
    void "tables wrapped in body-level IF fields become conditional rows of the preceding table"() {
        // given
        def migration = mockMigration()
        def displayRules = migration.displayRuleRepository

        def document = new XWPFDocument()
        document.createStyles()
        def headerTable = createTable(document, ["Benefit", "Amount"])
        setGrid(headerTable, [2190L, 1525L])
        // ¶ IF «Ben02Type» = "SCH" " IF «BenefitCount» > 1 "   <TABLE A>   ¶ "" ""  [sep]   <TABLE B (cached result)>   ¶ [end] [end]
        def opening = document.createParagraph()
        fieldCharacter(opening.createRun(), STFldCharType.BEGIN)
        instruction(opening.createRun(), " if ")
        appendMergeField(opening, " MERGEFIELD Ben02Type ")
        instruction(opening.createRun(), ' = "SCH" ')
        fieldCharacter(opening.createRun(), STFldCharType.BEGIN)
        instruction(opening.createRun(), " if ")
        appendMergeField(opening, " MERGEFIELD BenefitCount ", "2")
        instruction(opening.createRun(), ' > 1 "')
        def conditionalTable = createTable(document, ["", "596.75"])
        // Word stores the text of a table nested in a field instruction as instrText; grid widths drift by a twip.
        instruction(conditionalTable.getRow(0).getCell(0).paragraphs[0].createRun(), "Ben02")
        setGrid(conditionalTable, [2191L, 1525L])
        def middle = document.createParagraph()
        instruction(middle.createRun(), '"" "" ')
        fieldCharacter(middle.createRun(), STFldCharType.SEPARATE)
        def cachedResultTable = createTable(document, ["Ben02 cached", "596.75"])
        def closing = document.createParagraph()
        fieldCharacter(closing.createRun(), STFldCharType.END)
        instruction(closing.createRun(), " ")
        fieldCharacter(closing.createRun(), STFldCharType.END)
        def trailing = document.createParagraph()
        trailing.createRun().setText("After the table")

        // when
        def body = new DocxBodyContent(migration, "sample", "sample_page1")
        def state = new FieldParseState(conditionalTableHandler: body.&addConditionalTable)
        def headerBuilder = DocxTableParser.createTableBuilder(migration, headerTable, true)
        DocxTableParser.addRows(migration, headerBuilder, headerTable, headerTable.rows, "sample", 2, true)
        body.addTable(headerTable, headerBuilder, 2)
        document.bodyElements.each { elem ->
            if (elem instanceof XWPFParagraph) {
                def paragraph = DocxParagraphParser.parseFlowParagraph(migration, elem, "sample", state)
                if (paragraph != null) {
                    body.add(paragraph)
                }
            } else if (elem instanceof XWPFTable && elem != headerTable) {
                if (state.depth > 0) {
                    DocxMergeFields.handleTableInsideField(state, elem)
                } else {
                    body.add(DocxTableParser.buildTable(migration, elem, elem.rows, "sample", 2, true))
                }
            }
        }
        def content = body.flow()

        // then
        assert content.size() == 2
        Table table = content[0] as Table
        assert table.rows.size() == 2
        assert table.rows[0].displayRuleRef == null
        assert table.rows[1].displayRuleRef.id.startsWith("docx_if_")
        assert table.rows[1].cells[0].content[0].content[0].content[0].value == "Ben02"
        assert content[1].content[0].content[0].value == "After the table"
        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules).upsert(ruleCaptor.capture())
        assert ruleCaptor.value.name == 'Ben02Type = "SCH" AND BenefitCount > 1'

        document.close()
    }

    @Test
    void "conditional table not matching the preceding table is wrapped in an internal block with the rule"() {
        // given
        def migration = mockMigration()
        def displayRules = migration.displayRuleRepository
        def documentObjects = migration.documentObjectRepository

        def document = new XWPFDocument()
        document.createStyles()
        // ¶ IF «GmpPayable» = "Y" "  <TABLE>  ¶ "" [end]
        def opening = document.createParagraph()
        fieldCharacter(opening.createRun(), STFldCharType.BEGIN)
        instruction(opening.createRun(), " if ")
        appendMergeField(opening, " MERGEFIELD GmpPayable ")
        instruction(opening.createRun(), ' = "Y" "')
        def table = createTable(document, ["", "Meaning"])
        instruction(table.getRow(0).getCell(0).paragraphs[0].createRun(), "Technical word")
        def closing = document.createParagraph()
        instruction(closing.createRun(), '" "" ')
        fieldCharacter(closing.createRun(), STFldCharType.END)

        // when
        def body = new DocxBodyContent(migration, "sample", "sample_page3")
        def state = new FieldParseState(conditionalTableHandler: body.&addConditionalTable)
        document.bodyElements.each { elem ->
            if (elem instanceof XWPFParagraph) {
                def paragraph = DocxParagraphParser.parseFlowParagraph(migration, elem, "sample", state)
                if (paragraph != null) {
                    body.add(paragraph)
                }
            } else if (elem instanceof XWPFTable) {
                DocxMergeFields.handleTableInsideField(state, elem)
            }
        }
        def content = body.flow()

        // then
        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules).upsert(ruleCaptor.capture())
        assert ruleCaptor.value.name == 'GmpPayable = "Y"'
        assert content.size() == 1
        DocumentObjectRef ref = content[0] as DocumentObjectRef
        assert ref.id == "sample_page3_table1"
        assert ref.displayRuleRef.id == ruleCaptor.value.id
        def blockCaptor = ArgumentCaptor.forClass(DocumentObject)
        verify(documentObjects).upsert(blockCaptor.capture())
        assert blockCaptor.value.id == "sample_page3_table1"
        assert blockCaptor.value.name == "Technical word table"
        assert blockCaptor.value.internal
        Table blockTable = blockCaptor.value.content[0] as Table
        assert blockTable.rows.size() == 1
        assert blockTable.rows[0].displayRuleRef == null
        assert blockTable.rows[0].cells[0].content[0].content[0].content[0].value == "Technical word"

        document.close()
    }

    private static XWPFTable createTable(XWPFDocument document, List<String> cellTexts) {
        XWPFTable table = document.createTable(1, cellTexts.size())
        cellTexts.eachWithIndex { String cellText, int i -> table.getRow(0).getCell(i).setText(cellText) }
        return table
    }

    private static void setGrid(XWPFTable table, List<Long> widths) {
        def grid = table.CTTbl.tblGrid ?: table.CTTbl.addNewTblGrid()
        widths.each { grid.addNewGridCol().w = BigInteger.valueOf(it) }
    }
}
