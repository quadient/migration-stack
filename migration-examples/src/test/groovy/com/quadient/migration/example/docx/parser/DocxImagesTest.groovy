package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.Size
import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.xwpf.usermodel.XWPFPictureData
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

    private static XWPFPictureData picture(PictureType type, Long checksum) {
        def data = mock(XWPFPictureData)
        when(data.pictureTypeEnum).thenReturn(type)
        when(data.checksum).thenReturn(checksum)
        when(data.data).thenReturn([1, 2, 3] as byte[])
        return data
    }
}
