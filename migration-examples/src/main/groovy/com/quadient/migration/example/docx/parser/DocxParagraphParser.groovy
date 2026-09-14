package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFFieldRun
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.xmlbeans.XmlObject
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField

import static com.quadient.migration.example.docx.style.DocxParagraphStyles.captureParagraphStyle
import static com.quadient.migration.example.docx.style.DocxTextStyles.captureTextStyle

class ParagraphContentCollector {
    private final Migration migration
    private final String fileName
    private final Set<String> excludedImageEmbedIds

    private final List<ParagraphBuilder.TextBuilder> textBuilders = []
    // Same-styled text runs and adjacent IF fields are buffered until a run that does not belong to the group arrives;
    // a run is either text or field, so at most one of the two buffers is non-empty at any time.
    private final StringBuilder text = new StringBuilder()
    private String textStyleId
    // The field state may be shared by consecutive paragraphs (cell, body flow) so that fields spanning paragraphs resolve.
    private final FieldParseState fieldState
    // POI exposes the cached result runs inside w:fldSimple as ordinary paragraph runs.  Remember fields already
    // resolved so that the cached «name» text does not become literal content (or get emitted once per result run).
    private final Set<CTSimpleField> resolvedSimpleFields = Collections.newSetFromMap(new IdentityHashMap<CTSimpleField, Boolean>())
    private boolean containsFieldCode = false

    ParagraphContentCollector(Migration migration, String fileName, Set<String> excludedImageEmbedIds = Collections.emptySet(),
                              FieldParseState fieldState = null) {
        this.migration = migration
        this.fileName = fileName
        this.excludedImageEmbedIds = excludedImageEmbedIds
        this.fieldState = fieldState ?: new FieldParseState()
    }

    void addRun(XWPFRun run, String styleId) {
        if (!run.embeddedPictures.isEmpty()) {
            flushText()
            flushFields()
            DocxImages.processRunImages(migration, run, fileName, textBuilders, excludedImageEmbedIds)
        }

        CTSimpleField simpleField = run instanceof XWPFFieldRun ? (run as XWPFFieldRun).CTField : null
        if (simpleField != null && !resolvedSimpleFields.contains(simpleField)) {
            String instruction = simpleField.instr
            if (DocxMergeFields.isMergeField(instruction)) {
                resolvedSimpleFields.add(simpleField)
                flushText()
                flushFields()
                DocxMergeFields.addMergeField(migration, textBuilders, fileName, instruction, styleId)
                return
            }
        }

        List<XmlObject> fieldChildren = DocxMergeFields.fieldChildren(run)
        if (fieldState.depth == 0 && fieldChildren.every { DocxMergeFields.isInstructionText(it) }) {
            flushFields()
            // Word marks every run inside a field instruction as instrText, including the plain text of a table
            // wrapped in a body-level IF field. Outside any open field such text is ordinary content.
            appendText(fieldChildren.isEmpty() ? run.text() : DocxMergeFields.instructionText(fieldChildren), styleId)
            return
        }
        containsFieldCode = true
        if (fieldState.depth == 0 && fieldChildren.any { DocxMergeFields.isFieldBegin(it) }) {
            flushText()
        }
        DocxMergeFields.handleFieldChildren(migration, fieldChildren, styleId, fieldState, textBuilders, fileName)
    }

    List<ParagraphBuilder.TextBuilder> finish() {
        flushText()
        flushFields()
        return textBuilders
    }

    // True for a paragraph that carried only field code (instructions, cached results) and produced no content.
    boolean isFieldCodeOnly() {
        return containsFieldCode && textBuilders.isEmpty()
    }

    private void appendText(String runText, String styleId) {
        if (!runText) {
            return
        }
        if (text.length() > 0 && styleId != textStyleId) {
            flushText()
        }
        textStyleId = styleId
        text.append(runText)
    }

    private void flushText() {
        if (text.length() == 0) {
            return
        }
        DocxVariablePatterns.addText(migration, textBuilders, text.toString(), textStyleId, fileName)
        text.setLength(0)
    }

    private void flushFields() {
        DocxMergeFields.flushPendingIfFields(migration, fieldState, textBuilders, fileName)
    }
}

static Paragraph parseParagraph(Migration migration, XWPFParagraph paragraph, String fileName, String context = null,
                                Set<String> excludedImageEmbedIds = Collections.emptySet()) {
    FieldParseState fieldState = new FieldParseState()
    Paragraph parsed = parseFlowParagraph(migration, paragraph, fileName, fieldState, context, excludedImageEmbedIds)
    warnUnterminatedField(fieldState, "paragraph: '${paragraph.text}'")
    return parsed ?: buildParagraph(migration, paragraph, fileName, context, [])
}

// Parses a paragraph that belongs to a flow of paragraphs sharing one field state (table cell, page body). Returns null
// for paragraphs that carried nothing but field code, e.g. the instruction part of an IF continued in the next paragraph.
static Paragraph parseFlowParagraph(Migration migration, XWPFParagraph paragraph, String fileName, FieldParseState fieldState,
                                    String context = null, Set<String> excludedImageEmbedIds = Collections.emptySet()) {
    String paragraphStyleId = paragraph.styleID ?: "unknown"
    ParagraphContentCollector collector = new ParagraphContentCollector(migration, fileName, excludedImageEmbedIds, fieldState)

    paragraph.runs.each { XWPFRun run ->
        collector.addRun(run, captureTextStyle(migration, run, fileName, paragraphStyleId, context))
    }

    List<ParagraphBuilder.TextBuilder> content = collector.finish()
    if (collector.fieldCodeOnly) {
        return null
    }
    return buildParagraph(migration, paragraph, fileName, context, content)
}

static void warnUnterminatedField(FieldParseState fieldState, String location) {
    if (fieldState.depth > 0) {
        println "  Warning: Unterminated complex field (missing fldChar end) in ${location}"
        fieldState.stack.clear()
    }
}

private static Paragraph buildParagraph(Migration migration, XWPFParagraph paragraph, String fileName, String context,
                                        List<ParagraphBuilder.TextBuilder> content) {
    ParagraphBuilder paragraphBuilder = new ParagraphBuilder().content(content)
    String styleId = captureParagraphStyle(migration, paragraph, fileName, paragraph.styleID ?: "unknown", context)
    if (styleId) {
        paragraphBuilder.styleRef(styleId)
    }
    return paragraphBuilder.build()
}
