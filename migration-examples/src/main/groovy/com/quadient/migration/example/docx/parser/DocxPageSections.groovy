package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.IBodyElement
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STSectionMark

class DocxSection {
    List<IBodyElement> elements = []
    CTSectPr sectPr
}

class DocxPage {
    int index
    List<DocxSection> sections = []
    Position contentPosition
    Size width
    Size height
    List<DocumentContent> content = []

    List<IBodyElement> bodyElements() {
        return sections.collectMany { it.elements }
    }

    String id(String fileName) {
        return "${fileName}_page${index + 1}"
    }

    List<XWPFParagraph> paragraphs() {
        return bodyElements().findAll { it instanceof XWPFParagraph } as List<XWPFParagraph>
    }
}

class DocxPageSections {
    static List<DocxSection> splitIntoSections(XWPFDocument doc) {
        List<DocxSection> sections = []
        DocxSection current = new DocxSection()
        doc.bodyElements.each { IBodyElement elem ->
            current.elements.add(elem)
            CTSectPr sectPr = elem instanceof XWPFParagraph ? elem.CTP.PPr?.sectPr : null
            if (sectPr != null) {
                current.sectPr = sectPr
                sections.add(current)
                current = new DocxSection()
            }
        }
        if (!current.elements.isEmpty()) {
            current.sectPr = doc.document?.body?.sectPr
            sections.add(current)
        }
        return sections
    }

    static List<DocxPage> groupIntoPages(List<DocxSection> sections) {
        List<DocxPage> pages = []
        sections.eachWithIndex { DocxSection section, int i ->
            // A section can contain Word's cached pagination marker.  It is not an authored page break, so only
            // honor it where it begins a paragraph; using a marker after text would move already-laid-out content.
            splitAtRenderedPageBreaks(section).eachWithIndex { DocxSection fragment, int fragmentIndex ->
                if ((i == 0 && fragmentIndex == 0) || fragmentIndex > 0 || startsNewPage(section.sectPr)) {
                    pages.add(newPage(pages.size(), fragment))
                } else {
                    pages.last().sections.add(fragment)
                }
            }
        }
        return pages
    }

    private static List<DocxSection> splitAtRenderedPageBreaks(DocxSection section) {
        if (!section.elements.any { it instanceof XWPFParagraph && beginsAfterRenderedPageBreak(it) }) {
            // Preserve the original section object when no cached pagination is involved.
            return [section]
        }
        List<DocxSection> fragments = []
        DocxSection current = new DocxSection(sectPr: section.sectPr)
        section.elements.each { IBodyElement element ->
            if (element instanceof XWPFParagraph && beginsAfterRenderedPageBreak(element) && !current.elements.isEmpty()) {
                fragments.add(current)
                current = new DocxSection(sectPr: section.sectPr)
            }
            current.elements.add(element)
        }
        if (!current.elements.isEmpty()) {
            fragments.add(current)
        }
        return fragments
    }

    private static boolean beginsAfterRenderedPageBreak(XWPFParagraph paragraph) {
        String xml = paragraph.CTP.xmlText()
        def marker = xml =~ /<(?:[A-Za-z_][\w.-]*:)?lastRenderedPageBreak(?=[\s\/>])/
        if (!marker.find()) {
            return false
        }
        // These are the WordprocessingML elements that produce visible paragraph content.  The marker is safe to
        // treat as a page boundary only when none of them precedes it.
        def visibleContent = xml =~ /<(?:[A-Za-z_][\w.-]*:)?(?:t|instrText|drawing|tab|br|object)(?=[\s\/>])/
        return !visibleContent.find() || marker.start() < visibleContent.start()
    }

    private static DocxPage newPage(int index, DocxSection firstSection) {
        List<Size> size = DocxPageLayout.resolvePageSize(firstSection.sectPr)
        return new DocxPage(
                index: index,
                sections: [firstSection],
                contentPosition: DocxPageLayout.resolvePageContentPosition(firstSection.sectPr),
                width: size[0],
                height: size[1],
        )
    }

    private static boolean startsNewPage(CTSectPr sectPr) {
        if (sectPr == null || !sectPr.isSetType()) {
            return true
        }
        def mark = sectPr.type.val
        return mark != STSectionMark.CONTINUOUS && mark != STSectionMark.NEXT_COLUMN
    }
}
