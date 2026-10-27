package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFSDT
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtBlock
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtRun

import static com.quadient.migration.example.Utils.mockMigration
import static org.mockito.Mockito.atLeastOnce
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

    @Test
    void "untagged inline control showing placeholder becomes a generated variable named by document and control id"() {
        // given: Word's default placeholder control without tag or title, between plain text runs
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def ctp = doc.createParagraph().CTP
            ctp.addNewR().addNewT().stringValue = 'For '
            def sdt = ctp.addNewSdt()
            sdt.addNewSdtPr().with {
                addNewId().val = BigInteger.valueOf(-160470844)
                addNewShowingPlcHdr()
                // the formatting a filled-in value gets
                addNewRPr().with {
                    addNewB()
                    addNewRFonts().ascii = 'Arial'
                }
            }
            def placeholderRun = sdt.addNewSdtContent().addNewR()
            placeholderRun.addNewRPr().addNewColor().val = '808080'
            placeholderRun.addNewT().stringValue = 'Click or tap here to enter text.'
            ctp.addNewR().addNewT().stringValue = ' years'
            def migration = mockMigration()

            // when
            def parsed = DocxParagraphParser.parseParagraph(migration, new XWPFParagraph(ctp, doc), 'PO192')

            // then: the placeholder is replaced by a styled variable reference, the surrounding text is kept
            assert parsed.content.collect { it.content[0] instanceof VariableRef ? it.content[0].id : it.content[0].value } ==
                    ['For ', 'PO192_ContentControl_-160470844', ' years']
            assert parsed.content.every { it.styleRef != null }
            def captor = ArgumentCaptor.forClass(Variable)
            verify(migration.variableRepository).upsert(captor.capture())
            assert captor.value.name == 'PO192_ContentControl_-160470844'

            // and: the variable is styled by the control's own formatting, not the grey placeholder run
            def styleCaptor = ArgumentCaptor.forClass(TextStyle)
            verify(migration.textStyleRepository, atLeastOnce()).upsert(styleCaptor.capture())
            def variableStyle = styleCaptor.allValues.find { it.id == parsed.content[1].styleRef.id }.definition
            assert variableStyle.bold
            assert variableStyle.fontFamily == 'Arial'
            assert variableStyle.foregroundColor == null
        }
    }

    @Test
    void "untagged block control becomes a generated variable when unfilled and keeps its text otherwise"() {
        new XWPFDocument().withCloseable { doc ->
            // given: an empty block control and one holding real text, neither tagged
            def empty = CTSdtBlock.Factory.newInstance()
            empty.addNewSdtPr().addNewId().val = BigInteger.valueOf(7)
            empty.addNewSdtContent()
            def filled = CTSdtBlock.Factory.newInstance()
            filled.addNewSdtPr().addNewId().val = BigInteger.valueOf(8)
            filled.addNewSdtContent().addNewP().addNewR().addNewT().stringValue = 'Static clause'
            def migration = mockMigration()

            // when
            def emptyParagraph = DocxContentControls.parseBlock(migration, new XWPFSDT(empty, doc), 'sample', empty.sdtPr)
            def filledParagraph = DocxContentControls.parseBlock(migration, new XWPFSDT(filled, doc), 'sample', filled.sdtPr)

            // then
            assert emptyParagraph.content[0].content[0].id == 'sample_ContentControl_7'
            assert filledParagraph.content[0].content[0].value.trim() == 'Static clause'
        }
    }
}
