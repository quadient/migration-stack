package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.apache.xmlbeans.XmlObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STAlignH
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromV

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static com.quadient.migration.example.Utils.mockMigration

class DocxAnchoredAreasTest {
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
