package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.Size
import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.xmlbeans.XmlObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static com.quadient.migration.example.Utils.mockMigration

class DocxImagesTest {
    @BeforeEach
    @AfterEach
    void resetImages() { DocxImages.resetImageState() }

    @ParameterizedTest
    @CsvSource(['PNG,.png', 'JPEG,.jpg', 'CMYKJPEG,.jpg', 'GIF,.gif', 'BMP,.bmp', 'DIB,.bmp', 'TIFF,.tiff', 'SVG,.svg'])
    void "supported images persist bytes and dimensions"(String type, String extension) {
        // given: a supported image with explicit dimensions
        def migration = mockMigration()
        def data = picture(PictureType.valueOf(type), 42L)
        def options = new ImageOptions(Size.ofPoints(120), Size.ofPoints(80))
        // when / then: registration assigns the first image identifier
        assert DocxImages.registerImageData(migration, data, 'sample', options) == 'sample_img_1'
        // then: bytes, source path and dimensions are persisted
        verify(migration.storage).write('sample_img_1' + extension, data.data)
        def captor = ArgumentCaptor.forClass(Image)
        verify(migration.imageRepository).upsert(captor.capture())
        assert captor.value.sourcePath == 'sample_img_1' + extension
        assert captor.value.options == options
    }

    @Test
    void "checksum deduplication avoids repeated writes and reset starts a new document"() {
        // given: two image objects with the same checksum
        def migration = mockMigration()
        def first = picture(PictureType.PNG, 42L)
        def duplicate = picture(PictureType.PNG, 42L)
        // when / then: registering both images returns the same identifier
        assert DocxImages.registerImageData(migration, first, 'sample', null) == 'sample_img_1'
        assert DocxImages.registerImageData(migration, duplicate, 'sample', null) == 'sample_img_1'
        // then: the duplicate causes no additional storage or repository write
        verify(migration.imageRepository, times(1)).upsert(any(Image))
        verify(migration.storage, times(1)).write('sample_img_1.png', first.data)
        // when: image state is reset for the next document
        DocxImages.resetImageState()
        // then: the same bytes are registered again, starting from image number one
        assert DocxImages.registerImageData(migration, duplicate, 'next', null) == 'next_img_1'
        verify(migration.imageRepository, times(2)).upsert(any(Image))
    }

    @Test
    void "unsupported images neither persist nor consume an image number"() {
        // given: fresh repositories and storage
        def migration = mockMigration()
        // when / then: an unsupported image is rejected
        assert DocxImages.registerImageData(migration, picture(PictureType.EMF, 1L), 'sample', null) == null
        // then: nothing is persisted
        verifyNoInteractions(migration.storage, migration.imageRepository)
        // when / then: the next supported image still receives the first identifier
        assert DocxImages.registerImageData(migration, picture(PictureType.PNG, 2L), 'sample', null) == 'sample_img_1'
    }

    @Test
    void "legacy VML relationship image is emitted inline with its shape dimensions"() {
        // given: the legacy picture form used by MV0407GX2.docx inside table-cell paragraphs
        def migration = mockMigration()
        byte[] bytes = [1, 2, 3] as byte[]
        new XWPFDocument().withCloseable { document ->
            String embedId = document.addPictureData(bytes, PictureType.PNG)
            def run = document.createParagraph().createRun()
            setVml(run, """<v:shape style="position:absolute;width:22.7pt;height:22.7pt">
                <v:imagedata r:id="${embedId}"/>
            </v:shape>""")
            List builders = []

            // when
            DocxImages.processRunImages(migration, run, 'sample', builders)

            // then: position offsets are deliberately ignored and the picture becomes paragraph content
            assert builders[0].build().content[0].id == 'sample_img_1'
            verify(migration.storage).write('sample_img_1.png', bytes)
            def captor = ArgumentCaptor.forClass(Image)
            verify(migration.imageRepository).upsert(captor.capture())
            assert [captor.value.options.resizeWidth, captor.value.options.resizeHeight]*.toPoints() == [22.7d, 22.7d]
        }
    }

    @Test
    void "paragraph collector keeps a legacy VML image before its table-cell title"() {
        // given: an anchored VML icon followed by its title, as stored in the sample's cells
        def migration = mockMigration()
        new XWPFDocument().withCloseable { document ->
            String embedId = document.addPictureData([1, 2, 3] as byte[], PictureType.PNG)
            def paragraph = document.createParagraph()
            def imageRun = paragraph.createRun()
            setVml(imageRun, """<v:shape style="position:absolute;width:22.7pt;height:22.7pt">
                <v:imagedata r:id="${embedId}"/>
            </v:shape>""")
            def titleRun = paragraph.createRun()
            titleRun.setText('DATOS COTIZACIÓN')
            def collector = new ParagraphContentCollector(migration, 'sample')

            // when
            collector.addRun(imageRun, 'title')
            collector.addRun(titleRun, 'title')
            def content = collector.finish()*.build()

            // then
            assert content[0].content[0].id == 'sample_img_1'
            assert content[1].content[0].value == 'DATOS COTIZACIÓN'
        }
    }

    @Test
    void "absolute VML image paragraph merges with the following cell title and drops layout spaces"() {
        // given: the separate image and title paragraphs used by MV0407GX2.docx
        def migration = mockMigration()
        new XWPFDocument().withCloseable { document ->
            document.createStyles()
            String embedId = document.addPictureData([1, 2, 3] as byte[], PictureType.PNG)
            def table = document.createTable(1, 1)
            def cell = table.getRow(0).getCell(0)
            def imageParagraph = cell.paragraphs[0]
            def imageRun = imageParagraph.createRun()
            setVml(imageRun, """<v:shape style="position:absolute;width:22.7pt;height:22.7pt">
                <v:imagedata r:id="${embedId}"/>
            </v:shape>""")
            imageParagraph.createRun().setText('            ')
            cell.addParagraph().createRun().setText('        DATOS  TOMADOR')

            // when
            def parsed = DocxTableParser.parseTable(migration, table, 'sample')
            def content = parsed.rows[0].cells[0].content

            // then: one paragraph contains the icon, one natural space, and the title
            assert content.size() == 1
            assert content[0].content[0].content[0].id == 'sample_img_1'
            assert content[0].content[1].content[0].value == ' DATOS  TOMADOR'
        }
    }

    @Test
    void "legacy VML vector shape is converted to an inline SVG image"() {
        // given: the legacy person-icon form used by MV0407GX2.docx
        def migration = mockMigration()
        new XWPFDocument().withCloseable { document ->
            def run = document.createParagraph().createRun()
            setVml(run, '<v:shape style="position:absolute;width:15.35pt;height:16pt" coordsize="243,265" path="m0,0l243,0,243,265,0,265xe" fillcolor="#016557" stroked="f"/>')
            List builders = []

            // when
            DocxImages.processRunImages(migration, run, 'sample', builders)

            // then
            assert builders[0].build().content[0].id == 'sample_img_1'
            def write = mockingDetails(migration.storage).invocations.find {
                it.method.name == 'write' && it.arguments[0] == 'sample_img_1.svg'
            }
            assert write != null
            String svg = new String(write.arguments[1] as byte[], 'UTF-8')
            assert svg.contains('viewBox="0 0 243 265"')
            assert svg.contains('d="M 0 0 L 243 0 L 243 265 L 0 265 Z"')
            assert svg.contains('fill="#016557" stroke="none"')
        }
    }

    @Test
    void "VML relative cubic paths preserve omitted zero coordinates"() {
        assert DocxVmlImages.vmlPathToSvgPath('m243,220v,14,-4,25,-12,33xe') ==
                'M 243 220 c 0 14 -4 25 -12 33 Z'
    }

    private static void setVml(def run, String shapeXml) {
        run.CTR.set(XmlObject.Factory.parse("""<xml-fragment
                xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                xmlns:v="urn:schemas-microsoft-com:vml"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <w:pict>${shapeXml}</w:pict>
        </xml-fragment>"""))
    }

    private static XWPFPictureData picture(PictureType type, Long checksum) {
        def data = mock(XWPFPictureData)
        when(data.pictureTypeEnum).thenReturn(type)
        when(data.checksum).thenReturn(checksum)
        when(data.data).thenReturn([1, 2, 3] as byte[])
        return data
    }
}
