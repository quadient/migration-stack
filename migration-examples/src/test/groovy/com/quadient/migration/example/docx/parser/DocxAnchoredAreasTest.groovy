package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.apache.xmlbeans.XmlObject
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STAlignH
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromV

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.Mockito.verify
import static com.quadient.migration.example.Utils.mockMigration

class DocxAnchoredAreasTest {
    @Test
    void "legacy VML text box becomes a positioned area with parsed merge fields"() {
        // given: the VML w:pict/v:shape form used by KB47 - Velkommen til GF Grænsen_Følgebrev.docx
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def paragraph = document.createParagraph()
            def run = paragraph.createRun()
            run.CTR.set(XmlObject.Factory.parse('''<xml-fragment xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:v="urn:schemas-microsoft-com:vml">
                <w:pict><v:shape id="address-box" style="position:absolute;margin-left:12pt;margin-top:18pt;width:120pt;height:36pt">
                    <v:textbox><w:txbxContent><w:p><w:r><w:fldChar w:fldCharType="begin"/></w:r>
                    <w:r><w:instrText xml:space="preserve"> MERGEFIELD customer_name </w:instrText></w:r>
                    <w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>Customer name</w:t></w:r>
                    <w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:txbxContent></v:textbox>
                </v:shape></w:pict>
            </xml-fragment>'''))
            def page = page()
            page.sections = [new DocxSection(elements: [paragraph])]
            def migration = mockMigration()

            // when
            def result = DocxAnchoredAreas.extract(migration, document, 'sample', [page])

            // then: VML CSS geometry is relative to the content origin and the cached merge value is ignored
            def area = result.floatingAreasByPage[0][0]
            assert [area.position.x, area.position.y, area.position.width, area.position.height]*.toPoints() == [52d, 78d, 120d, 36d]
            def blockCaptor = ArgumentCaptor.forClass(DocumentObject)
            verify(migration.documentObjectRepository).upsert(blockCaptor.capture())
            assert blockCaptor.value.content[0].content[0].content[0] == new VariableRef('customer_name')
        }
    }

    @Test
    void "VML fallback text box is not duplicated when its alternate content has an anchor"() {
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            def paragraph = document.createParagraph()
            setAnchorWithVmlFallback(paragraph.createRun())
            def page = page()
            page.sections = [new DocxSection(elements: [paragraph])]
            def migration = mockMigration()

            def result = DocxAnchoredAreas.extract(migration, document, 'sample', [page])

            assert result.floatingAreasByPage[0].size() == 1
            assert [result.floatingAreasByPage[0][0].position.x, result.floatingAreasByPage[0][0].position.y]*.toPoints() == [50d, 80d]
            verify(migration.documentObjectRepository).upsert(org.mockito.ArgumentMatchers.any(DocumentObject))
        }
    }

    @Test
    void "page-sized image is a background and the same embed is suppressed from floating areas across pages"() {
        // given: the same image appears as a small anchor and a duplicated background on another page
        DocxImages.resetImageState()
        try {
            new XWPFDocument().withCloseable { document ->
                def small = document.createParagraph()
                addPictureAnchor(small, 1, 100, 50)
                def background = document.createParagraph()
                addPictureAnchor(background, 2, 360, 480) // Exactly 60 percent on both axes.
                addPictureAnchor(background, 2, 360, 480) // Duplicate drawing in the same page.
                def firstPage = page()
                firstPage.sections = [new DocxSection(elements: [small])]
                def secondPage = page()
                secondPage.index = 1
                secondPage.sections = [new DocxSection(elements: [background])]
                def source = mock(XWPFDocument)
                def data = mock(XWPFPictureData)
                when(source.getPictureDataByID('rId1')).thenReturn(data)
                when(data.pictureTypeEnum).thenReturn(PictureType.PNG)
                when(data.checksum).thenReturn(42L)
                when(data.data).thenReturn([1, 2, 3] as byte[])
                def migration = mockMigration()
                // when: anchors are extracted across both pages
                def result = DocxAnchoredAreas.extract(migration, source, 'sample', [firstPage, secondPage])
                // then: only one full-page background is emitted and its embed is consumed
                assert result.backgroundAreasByPage.keySet() == [1] as Set
                assert result.backgroundAreasByPage[1].size() == 1
                assert result.floatingAreasByPage.isEmpty()
                assert result.consumedEmbedIds == ['rId1'] as Set
                def position = result.backgroundAreasByPage[1][0].position
                assert [position.x, position.y, position.width, position.height]*.toPoints() == [0d, 0d, 600d, 800d]
            }
        } finally {
            DocxImages.resetImageState()
        }
    }

    private static void addPictureAnchor(def paragraph, long id, long width, long height) {
        def anchor = paragraph.createRun().CTR.addNewDrawing().addNewAnchor()
        anchor.addNewDocPr().id = id
        anchor.addNewExtent().cx = width * 12700L
        anchor.extent.cy = height * 12700L
        def graphicData = anchor.addNewGraphic().addNewGraphicData()
        graphicData.set(XmlObject.Factory.parse('''<xml-fragment
                xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"
                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <pic:pic><pic:blipFill><a:blip r:embed="rId1"/></pic:blipFill></pic:pic>
        </xml-fragment>'''))
        graphicData.uri = 'http://schemas.openxmlformats.org/drawingml/2006/picture'
    }

    private static void setAnchorWithVmlFallback(def run) {
        run.CTR.set(XmlObject.Factory.parse('''<xml-fragment
                xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
                xmlns:v="urn:schemas-microsoft-com:vml"
                xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006">
            <mc:AlternateContent><mc:Choice Requires="wps"><w:drawing><wp:anchor>
                <wp:positionH relativeFrom="margin"><wp:posOffset>127000</wp:posOffset></wp:positionH>
                <wp:positionV relativeFrom="margin"><wp:posOffset>254000</wp:posOffset></wp:positionV>
                <wp:extent cx="1270000" cy="635000"/><a:graphic><a:graphicData uri="http://schemas.microsoft.com/office/word/2010/wordprocessingShape">
                <wps:wsp><wps:txbx><w:txbxContent><w:p><w:r><w:t>Address</w:t></w:r></w:p></w:txbxContent></wps:txbx></wps:wsp>
                </a:graphicData></a:graphic></wp:anchor></w:drawing></mc:Choice>
                <mc:Fallback><w:pict><v:shape id="address-box" style="position:absolute;margin-left:12pt;margin-top:18pt;width:120pt;height:36pt">
                <v:textbox><w:txbxContent><w:p><w:r><w:t>Address</w:t></w:r></w:p></w:txbxContent></v:textbox>
                </v:shape></w:pict></mc:Fallback></mc:AlternateContent>
            </xml-fragment>'''))
    }

    @ParameterizedTest
    @CsvSource(['page,10,20', 'margin,50,80'])
    void "anchor offsets use the selected coordinate origin"(String origin, double x, double y) {
        // given: an anchor with explicit offsets relative to the page or margins
        def anchor = anchor()
        anchor.addNewPositionH().relativeFrom = STRelFromH.Enum.forString(origin)
        anchor.positionH.posOffset = 127000
        anchor.addNewPositionV().relativeFrom = STRelFromV.Enum.forString(origin)
        anchor.positionV.posOffset = 254000
        // when
        def position = DocxAnchoredAreas.resolveAnchorPosition(anchor, page())
        // then
        assert [position.x, position.y, position.width, position.height]*.toPoints() == [x, y, 100d, 50d]
    }

    @ParameterizedTest
    @CsvSource(['page,left,0', 'page,center,250', 'page,right,500', 'margin,left,40', 'margin,center,240', 'margin,right,440', 'margin,outside,440'])
    void "horizontal alignment accounts for object width and available bounds"(String origin, String alignment, double x) {
        // given: an alignment and coordinate origin from the parameterized case
        def anchor = anchor()
        anchor.addNewPositionH().relativeFrom = STRelFromH.Enum.forString(origin)
        anchor.positionH.align = STAlignH.Enum.forString(alignment)
        // when / then: resolving the position places the anchor within the selected bounds
        assert DocxAnchoredAreas.resolveAnchorPosition(anchor, page()).x.toPoints() == x
    }

    @Test
    void "missing anchor geometry falls back to content origin and zero extent"() {
        // given / when: resolve an empty anchor against a page with margins
        def position = DocxAnchoredAreas.resolveAnchorPosition(CTAnchor.Factory.newInstance(), page())
        // then
        assert [position.x, position.y, position.width, position.height]*.toPoints() == [40d, 60d, 0d, 0d]
    }

    private static CTAnchor anchor() {
        def anchor = CTAnchor.Factory.newInstance()
        anchor.addNewExtent().cx = 1270000L
        anchor.extent.cy = 635000L
        return anchor
    }

    private static DocxPage page() {
        return new DocxPage(width: Size.ofPoints(600), height: Size.ofPoints(800),
                contentPosition: new Position(Size.ofPoints(40), Size.ofPoints(60), Size.ofPoints(500), Size.ofPoints(680)))
    }
}
