package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.Hyperlink
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.example.docx.util.DocxUtils
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static com.quadient.migration.example.docx.DocxFieldFixtures.*
import static com.quadient.migration.example.Utils.mockMigration

class DocxParagraphParserTest {

    @Test
    void "external hyperlinks become model hyperlinks while surrounding text remains plain"() {
        // given: a normal Word external hyperlink between unlinked text runs
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def paragraph = doc.createParagraph()
            paragraph.createRun().setText('Read ')
            paragraph.createHyperlinkRun('https://example.com/terms').setText('the terms')
            paragraph.createRun().setText(' before proceeding.')

            // when
            def parsed = DocxParagraphParser.parseParagraph(mockMigration(), paragraph, 'sample')

            // then: only the linked text is represented by Hyperlink content, in source order
            assert parsed.content*.content.flatten().collect { it instanceof Hyperlink ? it : it.value } == [
                    'Read ',
                    new Hyperlink('https://example.com/terms', 'the terms', null),
                    ' before proceeding.'
            ]
        }
    }

    @Test
    void "adjacent equal formatting coalesces while style changes preserve text order"() {
        // given: adjacent plain runs, a bold run and a final plain run
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def paragraph = doc.createParagraph()
            paragraph.createRun().setText('Hello ')
            paragraph.createRun().setText('world')
            def bold = paragraph.createRun()
            bold.setBold(true)
            bold.setText('!')
            paragraph.createRun().setText(' Again')
            // when
            def parsed = DocxParagraphParser.parseParagraph(mockMigration(), paragraph, 'sample')
            // then: equal formatting merges while formatting boundaries and text order survive
            assert parsed.content.size() == 3
            assert parsed.content.collect { it.content*.value.join() } == ['Hello world', '!', ' Again']
            assert DocxUtils.extractParagraphText(parsed) == 'Hello world! Again'
        }
    }

    @Test
    void "untagged inline content control keeps its text with a captured style"() {
        // given: a plain-text content control without tag or title, showing bold placeholder text
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def ctp = doc.createParagraph().CTP
            ctp.addNewR().addNewT().stringValue = 'Name: '
            def sdtRun = ctp.addNewSdt().addNewSdtContent().addNewR()
            sdtRun.addNewRPr().addNewB()
            sdtRun.addNewT().stringValue = 'Click here'
            def paragraph = new XWPFParagraph(ctp, doc)

            // when
            def parsed = DocxParagraphParser.parseParagraph(mockMigration(), paragraph, 'sample')

            // then: the control text is literal content with its own (bold) text style
            assert parsed.content.collect { it.content*.value.join() } == ['Name: ', 'Click here']
            assert parsed.content.every { it.styleRef != null }
            assert parsed.content[0].styleRef.id != parsed.content[1].styleRef.id
        }
    }

    @Test
    void "a missing text style leaves content unstyled instead of failing"() {
        // given: a text style id that could not be resolved (dynamic Groovy cannot pick a styleRef overload for null)
        def migration = mockMigration(docxVariablePatterns: [/<<(\w+)>>/])
        def content = []

        // when: every text builder path receives the null style
        DocxVariablePatterns.addText(migration, content, 'Dear <<Name>>,', null, 'sample')
        DocxVariablePatterns.addVariable(migration, content, 'Tag', null, 'sample')
        DocxMergeFields.addMergeField(migration, content, 'sample', ' MERGEFIELD Policy ', null)
        DocxMergeFields.addSystemField(migration, content, 'sample', 'systemDate', null)

        // then: all content is kept, just without a style
        def built = content*.build()
        assert built.collect { it.content[0] instanceof VariableRef ? it.content[0].id : it.content[0].value } ==
                ['Dear ', 'Name', ',', 'Tag', 'Policy', 'systemDate']
        assert built.every { it.styleRef == null }
    }

    @Test
    void "tabs line breaks and blank paragraphs survive parsing"() {
        // given: a run containing text separated by a tab and a line break
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def paragraph = doc.createParagraph()
            def run = paragraph.createRun()
            run.setText('A')
            run.addTab()
            run.setText('B')
            run.addBreak()
            run.setText('C')
            def migration = mockMigration()
            // when / then: parsing preserves the tab and line break
            assert DocxUtils.extractParagraphText(DocxParagraphParser.parseParagraph(migration, paragraph, 'sample')) == 'A\tB\nC'
            // when: an empty paragraph is parsed
            def blank = DocxParagraphParser.parseParagraph(migration, doc.createParagraph(), 'sample')
            // then: a blank paragraph is retained rather than discarded
            assert blank != null
            assert DocxUtils.extractParagraphText(blank) == ''
        }
    }

    @Test
    void "configured text markers are parsed as variables while surrounding text is retained"() {
        // Each pattern has a capture group for the variable name. These markers are authoring conventions, not DOCX fields.
        def migration = mockMigration(docxVariablePatterns: [/\$\{\s*([^}]+?)\s*}/, /<<\s*([^>]+?)\s*>>/])
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def paragraph = doc.createParagraph()
            paragraph.createRun().setText('Dear ${customer.name}, reference << PolicyNumber >>.')

            def parsed = DocxParagraphParser.parseParagraph(migration, paragraph, 'sample')

            assert parsed.content.collect { it.content[0] instanceof VariableRef ? it.content[0].id : it.content[0].value } ==
                    ['Dear ', 'customer.name', ', reference ', 'PolicyNumber', '.']
            def variableCaptor = ArgumentCaptor.forClass(Variable)
            verify(migration.variableRepository, times(2)).upsert(variableCaptor.capture())
            // dotted source paths stay in the id, but the name keeps only the last segment
            assert variableCaptor.allValues.collect { [it.id, it.name] } ==
                    [['customer.name', 'name'], ['PolicyNumber', 'PolicyNumber']]
        }
    }

    @Test
    void "parseParagraph flushes a final IF with split instructions and cached field results"() {
        // given
        def migration = mockMigration()
        def variables = migration.variableRepository
        def displayRules = migration.displayRuleRepository

        def document = new XWPFDocument()
        document.createStyles()
        def paragraph = document.createParagraph()
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " if ")
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " ")
        instruction(paragraph.createRun(), "MERGEFIELD Item0")
        instruction(paragraph.createRun(), "2")
        instruction(paragraph.createRun(), "Wording ")
        fieldCharacter(paragraph.createRun(), STFldCharType.SEPARATE)
        instruction(paragraph.createRun(), "No discount")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)
        instruction(paragraph.createRun(), ' = "')
        instruction(paragraph.createRun(), 'No discount')
        instruction(paragraph.createRun(), '" "Full price applies" "')
        instruction(paragraph.createRun(), 'Discount applies yearly')
        instruction(paragraph.createRun(), '"')
        fieldCharacter(paragraph.createRun(), STFldCharType.SEPARATE)
        paragraph.createRun().setText("Discount applies yearly")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)

        // when
        def parsedParagraph = DocxParagraphParser.parseParagraph(migration, paragraph, "sample")

        // then
        assert parsedParagraph.content.collect { it.content[0].value } ==
                ["Full price applies", "Discount applies yearly"]
        assert parsedParagraph.content*.displayRuleRef*.id.every { it.startsWith("docx_if_") }
        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules, times(2)).upsert(ruleCaptor.capture())
        assert ruleCaptor.allValues*.name as Set == [
                'Item02Wording = "No discount"',
                'Item02Wording <> "No discount"'
        ] as Set
        verify(variables).upsert(any())

        document.close()
    }

    @Test
    void "nested IF in an else branch spanning two cell paragraphs yields text with an AND rule"() {
        // given
        def migration = mockMigration()
        def displayRules = migration.displayRuleRepository

        def document = new XWPFDocument()
        document.createStyles()
        def cell = document.createTable(1, 1).getRow(0).getCell(0)
        def first = cell.paragraphs[0]
        // Mirrors Word output: IF «StartDate» > "20250401" «IF «Discount» = "NONE" "" "«IF «Category» = "BASIC" "" "¶text"»"»
        fieldCharacter(first.createRun(), STFldCharType.BEGIN)
        instruction(first.createRun(), " if ")
        appendMergeField(first, ' MERGEFIELD StartDate \\@"YYYYMMDD"')
        instruction(first.createRun(), ' > "20250401" ')
        fieldCharacter(first.createRun(), STFldCharType.BEGIN)
        instruction(first.createRun(), " IF ")
        appendMergeField(first, " MERGEFIELD Item01Discount ", "PCT10")
        instruction(first.createRun(), ' = "NONE" "" "')
        fieldCharacter(first.createRun(), STFldCharType.BEGIN)
        instruction(first.createRun(), " if ")
        appendMergeField(first, " MERGEFIELD Item01Category ", "PREMIUM")
        instruction(first.createRun(), ' = "BASIC" "" "')
        def second = cell.addParagraph()
        instruction(second.createRun(), "Your first payment covers the remaining months")
        // The closing quote (with the space before it) sits in a differently formatted run, as in the sample.
        def closingQuote = second.createRun()
        closingQuote.bold = true
        instruction(closingQuote, ' " ')
        fieldCharacter(second.createRun(), STFldCharType.END)
        instruction(second.createRun(), '" ')
        fieldCharacter(second.createRun(), STFldCharType.END)
        instruction(second.createRun(), ' ')
        fieldCharacter(second.createRun(), STFldCharType.END)

        // when
        def state = new FieldParseState()
        def parsedFirst = DocxParagraphParser.parseFlowParagraph(migration, first, "sample", state)
        def parsedSecond = DocxParagraphParser.parseFlowParagraph(migration, second, "sample", state)

        // then
        assert parsedFirst == null
        assert state.depth == 0
        assert parsedSecond.content.collect { it.content[0].value } == ["Your first payment covers the remaining months "]
        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules).upsert(ruleCaptor.capture())
        assert ruleCaptor.value.name == 'StartDate > "20250401" AND Item01Discount <> "NONE" AND Item01Category <> "BASIC"'
        assert parsedSecond.content[0].displayRuleRef.id == ruleCaptor.value.id
        assert ruleCaptor.value.definition.group.items.size() == 3

        document.close()
    }
}
