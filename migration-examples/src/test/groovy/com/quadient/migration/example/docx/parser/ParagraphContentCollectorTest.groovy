package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static com.quadient.migration.example.docx.DocxFieldFixtures.*
import static com.quadient.migration.example.Utils.mockMigration

class ParagraphContentCollectorTest {

    @Test
    void "collector creates display rule and conditional text from nested Word field"() {
        // given
        def migration = mockMigration()
        def variables = migration.variableRepository
        def displayRules = migration.displayRuleRepository

        def document = new XWPFDocument()
        def paragraph = document.createParagraph()
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " IF ")
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " MERGEFIELD PaymentFrequency ")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)
        instruction(paragraph.createRun(), ' = 4 "quarterly" "" ')
        fieldCharacter(paragraph.createRun(), STFldCharType.END)

        // when
        def collector = new ParagraphContentCollector(migration, "sample")
        paragraph.runs.each { collector.addRun(it, "body") }
        def textBuilders = collector.finish()

        // then
        assert textBuilders.size() == 1
        def text = textBuilders[0].build()
        assert text.content[0].value == "quarterly"
        assert text.styleRef.id == "body"
        assert text.displayRuleRef.id.startsWith("docx_if_")

        def ruleCaptor = ArgumentCaptor.forClass(DisplayRule)
        verify(displayRules).upsert(ruleCaptor.capture())
        Binary comparison = ruleCaptor.value.definition.group.items[0] as Binary
        assert comparison.left.value == "PaymentFrequency"
        assert comparison.operator == BinOp.Equals
        assert comparison.right.value == "4"
        verify(variables).upsert(any())

        document.close()
    }

    @Test
    void "collector preserves instruction and field-character order within one run"() {
        // given
        def migration = mockMigration()
        def document = new XWPFDocument()
        def paragraph = document.createParagraph()
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " IF ")
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        def mergeInstructionAndEnd = paragraph.createRun()
        instruction(mergeInstructionAndEnd, " MERGEFIELD GmpPayable ")
        fieldCharacter(mergeInstructionAndEnd, STFldCharType.END)
        instruction(paragraph.createRun(), ' = "Y" "GMP applies" "" ')
        fieldCharacter(paragraph.createRun(), STFldCharType.END)

        // when
        def collector = new ParagraphContentCollector(migration, "sample")
        paragraph.runs.each { collector.addRun(it, "body") }
        def textBuilders = collector.finish()

        // then
        assert textBuilders[0].build().content[0].value == "GMP applies"
        verify(migration.displayRuleRepository).upsert(any())

        document.close()
    }

    @Test
    void "collector continues to emit a simple MERGEFIELD variable reference"() {
        // given
        def migration = mockMigration()
        def document = new XWPFDocument()
        def paragraph = document.createParagraph()
        fieldCharacter(paragraph.createRun(), STFldCharType.BEGIN)
        instruction(paragraph.createRun(), " MERGEFIELD PolicyHolderTitle ")
        fieldCharacter(paragraph.createRun(), STFldCharType.END)

        // when
        def collector = new ParagraphContentCollector(migration, "sample")
        paragraph.runs.each { collector.addRun(it, "body") }
        def textBuilders = collector.finish()

        // then
        def text = textBuilders[0].build()
        assert text.content[0].id == "PolicyHolderTitle"
        assert text.styleRef.id == "body"
        assert text.displayRuleRef == null

        document.close()
    }

    @Test
    void "adjacent exclusive equality fields become one styled FirstMatch in source order"() {
        // given
        def migration = mockMigration()
        def document = new XWPFDocument()
        def paragraph = document.createParagraph()
        appendEqualityIfField(paragraph, "PaymentFrequency", "4", "quarterly")
        appendEqualityIfField(paragraph, "PaymentFrequency", "1", "annually")
        appendEqualityIfField(paragraph, "PaymentFrequency", "12", "monthly")

        // when
        def collector = new ParagraphContentCollector(migration, "sample")
        paragraph.runs.each { collector.addRun(it, "body") }
        def textBuilders = collector.finish()

        // then
        assert textBuilders.size() == 1
        def text = textBuilders[0].build()
        assert text.styleRef.id == "body"
        FirstMatch firstMatch = text.content[0] as FirstMatch
        assert firstMatch.cases*.name == [
                "PaymentFrequency = 4",
                "PaymentFrequency = 1",
                "PaymentFrequency = 12"
        ]
        assert firstMatch.cases.collect { it.content[0].value } == ["quarterly", "annually", "monthly"]
        verify(migration.displayRuleRepository, times(3)).upsert(any())

        document.close()
    }

    @Test
    void "adjacent conditions on different variables remain independent conditional text"() {
        // given
        def migration = mockMigration()
        def document = new XWPFDocument()
        def paragraph = document.createParagraph()
        appendEqualityIfField(paragraph, "PaymentFrequency", "4", "quarterly")
        appendEqualityIfField(paragraph, "GmpPayable", '"Y"', "GMP applies")

        // when
        def collector = new ParagraphContentCollector(migration, "sample")
        paragraph.runs.each { collector.addRun(it, "body") }
        def textBuilders = collector.finish()

        // then
        def texts = textBuilders*.build()
        assert texts.collect { it.content[0].value } == ["quarterly", "GMP applies"]
        assert texts*.displayRuleRef*.id.every { it.startsWith("docx_if_") }
        assert texts.collect { it.content[0] }.every { !(it instanceof FirstMatch) }

        document.close()
    }

    @Test
    void "plain text separates conditional sequences and retains source order"() {
        // given
        def migration = mockMigration()
        def document = new XWPFDocument()
        def paragraph = document.createParagraph()
        appendEqualityIfField(paragraph, "PaymentFrequency", "4", "quarterly")
        paragraph.createRun().setText(" or ")
        appendEqualityIfField(paragraph, "PaymentFrequency", "12", "monthly")

        // when
        def collector = new ParagraphContentCollector(migration, "sample")
        paragraph.runs.each { collector.addRun(it, "body") }
        def textBuilders = collector.finish()

        // then
        def texts = textBuilders*.build()
        assert texts.collect { it.content[0].value } == ["quarterly", " or ", "monthly"]
        assert texts[0].displayRuleRef != null
        assert texts[1].displayRuleRef == null
        assert texts[2].displayRuleRef != null
        assert texts.collect { it.content[0] }.every { !(it instanceof FirstMatch) }

        document.close()
    }
}
