package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Test
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr

import static com.quadient.migration.example.docx.DocxTestSupport.mockMigration

class DocxHeaderFootersTest {
    @Test
    void 'default header and footer become fixed areas at the page edges'() {
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def policy = new XWPFHeaderFooterPolicy(document)
            policy.createHeader(STHdrFtr.DEFAULT).createParagraph().createRun().setText('Header text')
            policy.createFooter(STHdrFtr.DEFAULT).createParagraph().createRun().setText('Footer text')
            def bytes = new ByteArrayOutputStream()
            document.write(bytes)
            new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray())).withCloseable { reopened ->
                assert reopened.headerList.size() == 1
                assert reopened.footerList.size() == 1
                reopened.document.body.sectPr.addNewPgMar().header = 720
                def page = new DocxPage(index: 0,
                        sections: [new DocxSection(sectPr: reopened.document.body.sectPr)],
                        width: Size.ofPoints(600), height: Size.ofPoints(800),
                        contentPosition: new Position(Size.ofPoints(40), Size.ofPoints(60), Size.ofPoints(500), Size.ofPoints(680)))

                def migration = mockMigration()
                def areas = DocxHeaderFooters.areas(migration, reopened, page, 'sample')

                assert areas.size() == 2
                assert areas[0].position.x.toPoints() == 40d
                assert areas[0].position.y.toPoints() == 36d
                assert areas[0].position.height.toPoints() == 24d
                assert areas[1].position.x.toPoints() == 40d
                assert areas[1].position.y.toPoints() == 740d
                assert areas[1].position.height.toPoints() == 60d
                assert areas*.content*.size() == [1, 1]
                assert areas[0].content[0].content[0].content[0].value == 'Header text'
                assert areas[1].content[0].content[0].content[0].value == 'Footer text'
            }
        }
    }
}
