package com.quadient.migration.example.docx.parser

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STSectionMark

class DocxPageSectionsTest {
    @Test
    void "section break belongs to preceding section and trailing content uses body properties"() {
        // given: paragraphs and a table separated from trailing content by a section break
        new XWPFDocument().withCloseable { doc ->
            def first = doc.createParagraph()
            def table = doc.createTable()
            def boundary = doc.createParagraph()
            def firstProperties = boundary.CTP.addNewPPr().addNewSectPr()
            def last = doc.createParagraph()
            def lastProperties = doc.document.body.addNewSectPr()
            // when
            def sections = DocxPageSections.splitIntoSections(doc)
            // then: the boundary paragraph stays in the first section
            assert sections*.elements == [[first, table, boundary], [last]]
            assert sections[0].sectPr.is(firstProperties)
            assert sections[1].sectPr.is(lastProperties)
            // when: the sections are grouped into pages
            def pages = DocxPageSections.groupIntoPages(sections)
            // then: page identifiers and ordered body elements are preserved
            assert pages*.id('sample') == ['sample_page1', 'sample_page2']
            assert pages[0].bodyElements() == [first, table, boundary]
            assert pages[0].paragraphs() == [first, boundary]
        }
    }

    @ParameterizedTest
    @CsvSource(['continuous,1', 'nextColumn,1', 'nextPage,2', 'evenPage,2', 'oddPage,2'])
    void "section type controls page grouping"(String mark, int count) {
        // given: two sections, with the second using the parameterized break type
        def properties = CTSectPr.Factory.newInstance()
        properties.addNewType().val = STSectionMark.Enum.forString(mark)
        def first = new DocxSection()
        def second = new DocxSection(sectPr: properties)
        // when
        def pages = DocxPageSections.groupIntoPages([first, second])
        // then: the break type determines page count without losing sections
        assert pages.size() == count
        assert pages.collectMany { it.sections } == [first, second]
    }

    @Test
    void "empty document and final section break do not create phantom sections"() {
        // given: an empty document
        new XWPFDocument().withCloseable { doc ->
            // when / then: splitting and grouping empty content produces no sections or pages
            assert DocxPageSections.splitIntoSections(doc).empty
            assert DocxPageSections.groupIntoPages([]).empty
            // when: the only paragraph ends with a section break
            doc.createParagraph().CTP.addNewPPr().addNewSectPr()
            // then: no extra trailing section is created
            assert DocxPageSections.splitIntoSections(doc).size() == 1
        }
    }

    @Test
    void "a rendered page break at the start of a paragraph creates a new page"() {
        // given: Word has cached a page boundary before the second paragraph's visible text
        new XWPFDocument().withCloseable { doc ->
            def first = doc.createParagraph()
            first.createRun().setText('First-page content')
            def second = doc.createParagraph()
            second.createRun().CTR.addNewLastRenderedPageBreak()
            second.createRun().setText('Second-page content')
            def third = doc.createParagraph()
            third.createRun().setText('More second-page content')
            doc.document.body.addNewSectPr()
            ByteArrayOutputStream serialized = new ByteArrayOutputStream()
            doc.write(serialized)

            new XWPFDocument(new ByteArrayInputStream(serialized.toByteArray())).withCloseable { reloaded ->
                // when
                def pages = DocxPageSections.groupIntoPages(DocxPageSections.splitIntoSections(reloaded))

                // then: the paragraph carrying the marker and subsequent content belong to the next page
                assert pages*.bodyElements()*.collect { it.text } == [['First-page content'], ['Second-page content', 'More second-page content']]
            }
        }
    }

    @Test
    void "a rendered page break after paragraph content is not used as a boundary"() {
        // given: a marker after content cannot be represented without splitting a paragraph
        new XWPFDocument().withCloseable { doc ->
            def paragraph = doc.createParagraph()
            paragraph.createRun().setText('Content before the cached marker')
            paragraph.createRun().CTR.addNewLastRenderedPageBreak()
            paragraph.createRun().setText('Content after the cached marker')
            doc.document.body.addNewSectPr()

            // when / then: preserve the paragraph as one flow item instead of guessing at an internal split
            assert DocxPageSections.groupIntoPages(DocxPageSections.splitIntoSections(doc))*.bodyElements() == [[paragraph]]
        }
    }

}
