package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import org.apache.poi.xwpf.usermodel.XWPFSDT
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtPr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DocxContentControls {
    private static final Logger log = LoggerFactory.getLogger(DocxContentControls)

    static String variableId(XWPFSDT control, CTSdtPr sdtPr = null, String fileName = null) {
        String named = control.tag?.trim() ?: control.title?.trim()
        if (named) return named
        if (!isUnfilled(control, sdtPr)) return null
        String controlId = sdtPr?.id?.val?.toString()
        if (!controlId || !fileName) return null
        String generated = "${fileName}_ContentControl_${controlId}"
        log.warn "  Warning: Content control without tag or title is mapped to generated variable '${generated}'."
        return generated
    }

    static boolean addInline(Migration migration, List<ParagraphBuilder.TextBuilder> textBuilders, XWPFSDT control,
                             String styleId, String fileName, CTSdtPr sdtPr = null) {
        String id = variableId(control, sdtPr, fileName)
        if (!id) return false
        DocxVariablePatterns.addVariable(migration, textBuilders, id, styleId, fileName)
        return true
    }

    static Paragraph parseBlock(Migration migration, XWPFSDT control, String fileName, CTSdtPr sdtPr = null) {
        List<ParagraphBuilder.TextBuilder> content = []
        String id = variableId(control, sdtPr, fileName)
        if (id) {
            DocxVariablePatterns.addVariable(migration, content, id, null, fileName)
        } else {
            String text = control.content?.text
            if (!text) return null
            content.add(new ParagraphBuilder.TextBuilder().string(text))
        }
        return new ParagraphBuilder().content(content).build()
    }

    private static boolean isUnfilled(XWPFSDT control, CTSdtPr sdtPr) {
        return sdtPr?.isSetShowingPlcHdr() || !control.content?.text?.trim()
    }
}
