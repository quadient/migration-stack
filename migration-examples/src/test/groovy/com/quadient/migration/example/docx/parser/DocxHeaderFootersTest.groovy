package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFRelation
import org.apache.xmlbeans.XmlObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromV

import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.openxml4j.opc.TargetMode

import static com.quadient.migration.example.Utils.mockMigration
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoInteractions
import static org.mockito.Mockito.atLeastOnce

class DocxHeaderFootersTest {
    @BeforeEach
    @AfterEach
    void resetImages() { DocxImages.resetImageState() }

    @Test
    void 'legacy VML header image is resolved from the header relationship and emitted as a fixed area'() {
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def header = new XWPFHeaderFooterPolicy(document).createHeader(STHdrFtr.DEFAULT)
            document.addPictureData([1, 2, 3] as byte[], PictureType.PNG)
            String headerImageId = 'rIdHeaderImage'
            header.packagePart.addRelationship(document.allPictures[0].packagePart.partName, TargetMode.INTERNAL,
                    XWPFRelation.IMAGES.relation, headerImageId)
            def run = header.createParagraph().createRun()
            setVml(run, """<v:shape style="position:absolute;width:122.25pt;height:35.25pt">
                <v:imagedata r:id="${headerImageId}"/>
            </v:shape>""")
            def bytes = new ByteArrayOutputStream()
            document.write(bytes)
            new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray())).withCloseable { reopened ->
                def page = new DocxPage(index: 0,
                        sections: [new DocxSection(sectPr: reopened.document.body.sectPr)],
                        width: Size.ofPoints(600), height: Size.ofPoints(800),
                        contentPosition: new Position(Size.ofPoints(40), Size.ofPoints(60), Size.ofPoints(500), Size.ofPoints(680)))

                def migration = mockMigration()
                def areas = DocxHeaderFooters.headerAreas(migration, reopened, page, 'sample')

                assert areas.size() == 1
                def blocks = capturedBlocks(migration, areas)
                assert blocks[0].content[0] instanceof ImageRef
                assert blocks[0].content[0].id == 'sample_img_1'
                assert [areas[0].position.width, areas[0].position.height]*.toPoints() == [122.25d, 35.25d]
            }
        }
    }

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
                def blocks = capturedBlocks(migration, areas)
                assert blocks*.id == ['sample_page1_header_area1', 'sample_page1_footer_area1']
                assert blocks*.name == ['sample Page 1 Header Area 1', 'sample Page 1 Footer Area 1']
                assert blocks[0].content[0].content[0].content[0].value == 'Header text'
                assert blocks[1].content[0].content[0].content[0].value == 'Footer text'
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    void 'mixed header and footer content preserves order and image geometry in separate blocks'(boolean headerOnly) {
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def policy = new XWPFHeaderFooterPolicy(document)
            def part = headerOnly ? policy.createHeader(STHdrFtr.DEFAULT) : policy.createFooter(STHdrFtr.DEFAULT)
            part.createParagraph().createRun().setText('Before table')
            part.createTable(1, 1).getRow(0).getCell(0).setText('Table cell')
            part.createParagraph().createRun().setText('After table')
            part.createParagraph().createRun().addPicture(new ByteArrayInputStream([1, 2, 3] as byte[]),
                    PictureType.PNG, 'image.png', 1270000, 635000)
            def bytes = new ByteArrayOutputStream()
            document.write(bytes)
            new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray())).withCloseable { reopened ->
                def migration = mockMigration()
                def page = page(reopened, 2)
                def areas = headerOnly ? DocxHeaderFooters.headerAreas(migration, reopened, page, 'sample') :
                        DocxHeaderFooters.footerAreas(migration, reopened, page, 'sample')

                assert areas.size() == 2
                def blocks = capturedBlocks(migration, areas)
                String partType = headerOnly ? 'header' : 'footer'
                assert blocks*.id == ["sample_page3_${partType}_area1", "sample_page3_${partType}_area2"]
                assert blocks[0].content*.class == [Paragraph, Table, Paragraph]
                assert blocks[0].content[0].content[0].content[0].value == 'Before table'
                assert blocks[0].content[1].rows[0].cells[0].content[0].content[0].content[0].value == 'Table cell'
                assert blocks[0].content[2].content[0].content[0].value == 'After table'
                assert blocks[1].content.size() == 1
                assert blocks[1].content[0] instanceof ImageRef
                assert blocks[1].content[0].id == 'sample_img_1'
                assert [areas[1].position.x, areas[1].position.y, areas[1].position.width,
                        areas[1].position.height]*.toPoints() == [40d, headerOnly ? 0d : 740d, 100d, 50d]
            }
        }
    }

    @Test
    void 'bundled DOCX samples persist header and footer blocks referenced by page areas'() {
        int headerFooterBlocks = 0
        File samples = new File('src', 'main')
        samples = new File(new File(samples, 'resources'), 'exampleResources')
        new File(samples, 'docx').eachFileMatch(~/.*\.docx/) { File file ->
            String fileName = file.name.substring(0, file.name.lastIndexOf('.'))
            def migration = mockMigration()
            def pages = DocxTemplateParser.parsePages(migration, file,
                    new DocumentObjectBuilder(fileName, DocumentObjectType.Template), fileName)
            def captor = ArgumentCaptor.forClass(DocumentObject)
            verify(migration.documentObjectRepository, atLeastOnce()).upsert(captor.capture())
            List<DocumentObject> blocks = captor.allValues.findAll { it.id ==~ /.*_page\d+_(header|footer)_area\d+/ }
            headerFooterBlocks += blocks.size()
            blocks.each { block ->
                assert block.type == DocumentObjectType.Block
                assert block.internal
                assert !block.content.isEmpty()
                assert block.originLocations == [fileName]
                assert pages.any { page ->
                    page.content.any { content ->
                        content instanceof Area && content.content.size() == 1 &&
                                content.content[0] instanceof DocumentObjectRef && content.content[0].id == block.id
                    }
                }
            }
        }
        assert headerFooterBlocks > 0
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    void 'anchored header and footer image blocks retain absolute page offsets'(boolean headerOnly) {
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def policy = new XWPFHeaderFooterPolicy(document)
            def part = headerOnly ? policy.createHeader(STHdrFtr.DEFAULT) : policy.createFooter(STHdrFtr.DEFAULT)
            String embedId = part.addPictureData([1, 2, 3] as byte[], PictureType.PNG)
            def anchor = part.createParagraph().createRun().CTR.addNewDrawing().addNewAnchor()
            anchor.addNewDocPr().id = 1
            anchor.addNewExtent().cx = 1270000
            anchor.extent.cy = 635000
            anchor.addNewPositionH().relativeFrom = STRelFromH.PAGE
            anchor.positionH.posOffset = 127000
            anchor.addNewPositionV().relativeFrom = STRelFromV.PAGE
            anchor.positionV.posOffset = 254000
            def graphicData = anchor.addNewGraphic().addNewGraphicData()
            graphicData.set(XmlObject.Factory.parse("""<xml-fragment
                    xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"
                    xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <pic:pic>
                    <pic:nvPicPr><pic:cNvPr id="1" name="image.png"/><pic:cNvPicPr/></pic:nvPicPr>
                    <pic:blipFill><a:blip r:embed="${embedId}"/></pic:blipFill>
                    <pic:spPr><a:xfrm><a:ext cx="1270000" cy="635000"/></a:xfrm></pic:spPr>
                </pic:pic>
            </xml-fragment>"""))
            graphicData.uri = 'http://schemas.openxmlformats.org/drawingml/2006/picture'
            def bytes = new ByteArrayOutputStream()
            document.write(bytes)
            new XWPFDocument(new ByteArrayInputStream(bytes.toByteArray())).withCloseable { reopened ->
                def migration = mockMigration()
                def areas = headerOnly ? DocxHeaderFooters.headerAreas(migration, reopened, page(reopened), 'sample') :
                        DocxHeaderFooters.footerAreas(migration, reopened, page(reopened), 'sample')

                assert areas.size() == 1
                def blocks = capturedBlocks(migration, areas)
                assert blocks[0].content.size() == 1
                assert blocks[0].content[0] instanceof ImageRef
                assert blocks[0].content[0].id == 'sample_img_1'
                assert [areas[0].position.x, areas[0].position.y, areas[0].position.width,
                        areas[0].position.height]*.toPoints() == [10d, 20d, 100d, 50d]
            }
        }
    }

    @Test
    void 'empty header and footer do not create areas or blocks'() {
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def policy = new XWPFHeaderFooterPolicy(document)
            policy.createHeader(STHdrFtr.DEFAULT)
            policy.createFooter(STHdrFtr.DEFAULT)
            def migration = mockMigration()

            assert DocxHeaderFooters.areas(migration, document, page(document), 'sample').isEmpty()
            verifyNoInteractions(migration.documentObjectRepository)
        }
    }

    private static List<DocumentObject> capturedBlocks(def migration, List areas) {
        def captor = ArgumentCaptor.forClass(DocumentObject)
        verify(migration.documentObjectRepository, times(areas.size())).upsert(captor.capture())
        List<DocumentObject> blocks = captor.allValues
        areas.eachWithIndex { area, int index ->
            assert area.content.size() == 1
            assert area.content[0] instanceof DocumentObjectRef
            assert area.content[0].id == blocks[index].id
        }
        assert blocks.every { it.type == DocumentObjectType.Block && it.internal && it.originLocations == ['sample'] }
        assert blocks*.id.toSet().size() == blocks.size()
        return blocks
    }

    private static DocxPage page(XWPFDocument document, int index = 0) {
        return new DocxPage(index: index,
                sections: [new DocxSection(sectPr: document.document.body.sectPr)],
                width: Size.ofPoints(600), height: Size.ofPoints(800),
                contentPosition: new Position(Size.ofPoints(40), Size.ofPoints(60), Size.ofPoints(500), Size.ofPoints(680)))
    }

    private static void setVml(def run, String shapeXml) {
        run.CTR.set(XmlObject.Factory.parse("""<xml-fragment
                xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                xmlns:v="urn:schemas-microsoft-com:vml"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <w:pict>${shapeXml}</w:pict>
        </xml-fragment>"""))
    }
}
