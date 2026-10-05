package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFSDT
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtRun

import static com.quadient.migration.example.Utils.mockMigration
import static org.mockito.Mockito.verify

class DocxContentControlsTest {
    @Test
    void "content-control tag becomes a string variable reference"() {
        def source = CTSdtRun.Factory.newInstance()
        source.addNewSdtPr().addNewTag().val = 'F_SINIESTRO'
        def document = new XWPFDocument()
        def control = new XWPFSDT(source, document)
        def migration = mockMigration()
        def content = [] as List<ParagraphBuilder.TextBuilder>

        assert DocxContentControls.addInline(migration, content, control, null, 'sample.docx')
        assert content[0].content[0].id == 'F_SINIESTRO'
        def captor = ArgumentCaptor.forClass(Variable)
        verify(migration.variableRepository).upsert(captor.capture())
        assert captor.value.id == 'F_SINIESTRO'
        document.close()
    }
}
