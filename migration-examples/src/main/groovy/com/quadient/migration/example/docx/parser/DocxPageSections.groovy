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
            // lastRenderedPageBreak is Word's cached layout output, not an authored page boundary.  Mapping it to
            // fixed design-time pages makes a single flowing template repeat headers only for the cached pages.
            if (i == 0 || startsNewPage(section.sectPr)) {
                pages.add(newPage(pages.size(), section))
            } else {
                pages.last().sections.add(section)
            }
        }
        return pages
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
