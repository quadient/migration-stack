package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import org.apache.poi.xwpf.usermodel.XWPFSDT

class DocxContentControls {
    static String variableId(XWPFSDT control) {
        return control.tag?.trim() ?: control.title?.trim()
    }

    static boolean addInline(Migration migration, List<ParagraphBuilder.TextBuilder> textBuilders, XWPFSDT control,
                             String styleId, String fileName) {
        String id = variableId(control)
        if (!id) return false
        DocxVariablePatterns.addVariable(migration, textBuilders, id, styleId, fileName)
        return true
    }

    static Paragraph parseBlock(Migration migration, XWPFSDT control, String fileName) {
        String id = variableId(control)
        if (!id) return null
        List<ParagraphBuilder.TextBuilder> content = []
        DocxVariablePatterns.addVariable(migration, content, id, null, fileName)
        return new ParagraphBuilder().content(content).build()
    }
}
