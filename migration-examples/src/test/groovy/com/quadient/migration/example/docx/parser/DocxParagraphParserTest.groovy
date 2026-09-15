package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.example.docx.util.DocxUtils
import org.apache.poi.xwpf.usermodel.XWPFDocument
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
            verify(migration.variableRepository, times(2)).upsert(any())
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
        instruction(paragraph.createRun(), "MERGEFIELD BEN0")
        instruction(paragraph.createRun(), "2")
        instruction(paragraph.createRun(), "EscWording ")
        fieldCharacter(paragraph.createRun(), STFldCharType.SEPARATE)
        instruction(paragraph.createRun(), "No increase will apply")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)
        instruction(paragraph.createRun(), ' = "')
        instruction(paragraph.createRun(), 'No increase will apply')
        instruction(paragraph.createRun(), '" "Pension will not increase" "')
        instruction(paragraph.createRun(), '1 April each year')
        instruction(paragraph.createRun(), '"')
        fieldCharacter(paragraph.createRun(), STFldCharType.SEPARATE)
        paragraph.createRun().setText("1 April each year")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)

        // when
        def parsedParagraph = DocxParagraphParser.parseParagraph(migration, paragraph, "sample")

        // then
        assert parsedParagraph.content.collect { it.content[0].value } ==
                ["Pension will not increase", "1 April each year"]
        assert parsedParagraph.content*.displayRuleRef*.id.every { it.startsWith("docx_if_") }
        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules, times(2)).upsert(ruleCaptor.capture())
        assert ruleCaptor.allValues*.name as Set == [
                'BEN02EscWording = "No increase will apply"',
                'BEN02EscWording <> "No increase will apply"'
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
        // Mirrors Word output: IF «PCD» > "20250401" «IF «Increase» = "FIX0" "" "«IF «Element» = "PRE88GMP" "" "¶text"»"»
        fieldCharacter(first.createRun(), STFldCharType.BEGIN)
        instruction(first.createRun(), " if ")
        appendMergeField(first, ' MERGEFIELD PensionCommencementDate \\@"YYYYMMDD"')
        instruction(first.createRun(), ' > "20250401" ')
        fieldCharacter(first.createRun(), STFldCharType.BEGIN)
        instruction(first.createRun(), " IF ")
        appendMergeField(first, " MERGEFIELD Ben01Increase ", "C(0:3)7F")
        instruction(first.createRun(), ' = "FIX0" "" "')
        fieldCharacter(first.createRun(), STFldCharType.BEGIN)
        instruction(first.createRun(), " if ")
        appendMergeField(first, " MERGEFIELD Ben01Element ", "POS88GMP")
        instruction(first.createRun(), ' = "PRE88GMP" "" "')
        def second = cell.addParagraph()
        instruction(second.createRun(), "Your first increase will reflect the number of months")
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
        assert parsedSecond.content.collect { it.content[0].value } == ["Your first increase will reflect the number of months "]
        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules).upsert(ruleCaptor.capture())
        assert ruleCaptor.value.name == 'PensionCommencementDate > "20250401" AND Ben01Increase <> "FIX0" AND Ben01Element <> "PRE88GMP"'
        assert parsedSecond.content[0].displayRuleRef.id == ruleCaptor.value.id
        assert ruleCaptor.value.definition.group.items.size() == 3

        document.close()
    }
}
