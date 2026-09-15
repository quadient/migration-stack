package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType

import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static com.quadient.migration.example.docx.DocxTestSupport.mockMigration

class DocxSectionsTest {

    @Test
    void "heading level comes from the outline level of the style chain"() {
        // given
        def document = new XWPFDocument()
        document.createStyles()
        addStyle(document, "Heading1", "heading 1", null, 0)
        addStyle(document, "SLRHDR1", "SLR HDR1", "Heading1", null)
        addStyle(document, "1SLRHDR1", "1. SLR HDR1", "SLRHDR1", null)
        addStyle(document, "TOCHeading", "TOC Heading", "Heading1", 9)
        addStyle(document, "SLRBody", "SLR Body", "Heading1", null)
        addStyle(document, "SLRURL", "SLR URL", "SLRBody", null)

        // when / then
        assert DocxHeadings.headingLevel(paragraph(document, "Heading1", "About us")) == 1
        assert DocxHeadings.headingLevel(paragraph(document, "SLRHDR1", "About us")) == 1
        assert DocxHeadings.headingLevel(paragraph(document, "1SLRHDR1", "About us")) == 1
        assert DocxHeadings.headingLevel(paragraph(document, "TOCHeading", "Contents")) == null
        assert DocxHeadings.headingLevel(paragraph(document, "SLRBody", "Body based on a heading")) == null
        assert DocxHeadings.headingLevel(paragraph(document, "SLRURL", "www.example.com")) == null
        assert DocxHeadings.headingLevel(paragraph(document, "SLRHDR1", "   ")) == null
        assert DocxHeadings.headingLevel(paragraph(document, null, "Body text")) == null

        XWPFParagraph copiedFormatting = paragraph(document, null, "Body text with a direct outline level")
        copiedFormatting.CTP.addNewPPr().addNewOutlineLvl().val = BigInteger.ZERO
        assert DocxHeadings.headingLevel(copiedFormatting) == null

        document.close()
    }

    @ParameterizedTest
    @CsvSource([
            "SLRHDR2, SLR HDR2, 2",
            "Heading3, heading 3, 3",
            "Title, Title, 1",
            "Subtitle, Subtitle, 2",
            "BodyText, Body Text, ",
            "TableParagraph, Table Paragraph, ",
    ])
    void "heading level falls back to the style name"(String styleId, String styleName, Integer expected) {
        // given
        def document = new XWPFDocument()
        document.createStyles()
        addStyle(document, styleId, styleName, null, null)

        // when / then
        assert DocxHeadings.headingLevel(paragraph(document, styleId, "Some heading")) == expected

        document.close()
    }

    @Test
    void "page content is divided into one block per top-level heading"() {
        // given
        def migration = mockMigration()
        def body = new DocxBodyContent(migration, "sample", "sample_page1")
        body.add(paragraph("Intro line"))
        body.add(paragraph("Your policy"), 1)
        body.add(paragraph("Policy details"), 2)
        body.add(paragraph("Some text"))
        body.add(paragraph("More information and help"), 1)
        body.add(paragraph(""))

        // when
        List content = body.sectionBlocks()

        // then
        assert content.size() == 3
        assert content.every { it instanceof DocumentObjectRef }
        assert content*.id == ["sample_page1_section1", "sample_page1_section2", "sample_page1_section3"]
        def captor = ArgumentCaptor.forClass(DocumentObject)
        verify(migration.documentObjectRepository, times(3)).upsert(captor.capture())
        List<DocumentObject> blocks = captor.allValues
        assert blocks*.name == ["sample_page1_section1", "Your policy", "More information and help"]
        assert blocks.every { it.internal }
        assert blocks[0].content.size() == 1
        assert blocks[1].content.size() == 3
        assert blocks[2].content.size() == 2
    }

    @Test
    void "lower level headings alone still divide the page when no higher level is present"() {
        // given
        def migration = mockMigration()
        def body = new DocxBodyContent(migration, "sample", "sample_page1")
        body.add(paragraph("What is in this pack?"), 2)
        body.add(paragraph("Text"))
        body.add(paragraph("What do you need to do?"), 2)

        // when / then
        assert body.sectionBlocks()*.id == ["sample_page1_section1", "sample_page1_section2"]
    }

    @Test
    void "page without headings stays a plain flow"() {
        // given
        def migration = mockMigration()
        def body = new DocxBodyContent(migration, "sample", "sample_page1")
        body.add(paragraph("Just text"))
        body.add(paragraph("And more"))

        // when
        List content = body.sectionBlocks()

        // then
        assert content.size() == 2
        assert content.every { it instanceof Paragraph }
        verify(migration.documentObjectRepository, times(0)).upsert(org.mockito.ArgumentMatchers.any())
    }

    @Test
    void "block names are shortened at a word boundary and fall back when empty"() {
        assert DocxBlocks.blockName("  Your   lifetime allowance ", "table", "x") == "Your lifetime allowance table"
        assert DocxBlocks.blockName("Explanation of the technical words", null, "x") == "Explanation of the technical words"
        assert DocxBlocks.blockName("", "table", "fallback") == "fallback"
        assert DocxBlocks.blockName(null, null, "fallback") == "fallback"
        String longText = "If you need more support or would like this information in Braille, large print or audio, please call"
        String name = DocxBlocks.blockName(longText, null, "x")
        assert name.length() <= 60
        assert name == "If you need more support or would like this information in"
    }

    private static Paragraph paragraph(String text) {
        def document = new XWPFDocument()
        document.createStyles()
        XWPFParagraph paragraph = document.createParagraph()
        paragraph.createRun().setText(text)
        return DocxParagraphParser.parseParagraph(mockMigration(), paragraph, "sample")
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String styleId, String text) {
        XWPFParagraph paragraph = document.createParagraph()
        if (styleId) {
            paragraph.style = styleId
        }
        paragraph.createRun().setText(text)
        return paragraph
    }

    private static void addStyle(XWPFDocument document, String styleId, String name, String basedOn, Integer outlineLevel) {
        CTStyle ctStyle = CTStyle.Factory.newInstance()
        ctStyle.type = STStyleType.PARAGRAPH
        ctStyle.styleId = styleId
        ctStyle.addNewName().val = name
        if (basedOn) {
            ctStyle.addNewBasedOn().val = basedOn
        }
        if (outlineLevel != null) {
            ctStyle.addNewPPr().addNewOutlineLvl().val = BigInteger.valueOf(outlineLevel)
        }
        document.styles.addStyle(new org.apache.poi.xwpf.usermodel.XWPFStyle(ctStyle, document.styles))
    }
}
