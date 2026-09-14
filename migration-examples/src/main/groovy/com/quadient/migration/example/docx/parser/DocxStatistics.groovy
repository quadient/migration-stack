package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import org.apache.poi.xwpf.usermodel.XWPFDocument

static void captureDocumentStatistics(XWPFDocument doc, DocumentObjectBuilder builder) {
    int shapeCount = doc.paragraphs.sum(0) { paragraph -> paragraph.runs.count { it.CTR.xmlText().contains("<w:drawing") } } as int
    int cellCount = doc.tables.sum(0) { table -> table.rows.sum(0) { it.tableCells.size() } } as int
    long textLength = (doc.paragraphs.sum(0) { it.text.length() } as long) + (doc.tables.sum(0) { it.text.length() } as long)

    addCountField(builder, "shapes", shapeCount)
    addCountField(builder, "headers", doc.headerList.size())
    addCountField(builder, "footers", doc.footerList.size())
    addCountField(builder, "images", doc.allPictures.size())
    addCountField(builder, "paragraphs", doc.paragraphs.count { it.text.length() > 0 } as int)
    addCountField(builder, "tables", doc.tables.size())
    if (doc.tables) {
        builder.addCustomField("cells", cellCount.toString())
    }
    addCountField(builder, "textLength", textLength)
}

private static void addCountField(DocumentObjectBuilder builder, String name, Number count) {
    if (count > 0) {
        builder.addCustomField(name, count.toString())
    }
}

static void captureDocxFileProperties(XWPFDocument doc, DocumentObjectBuilder builder) {
    def coreProps = doc.properties.coreProperties
    builder.addCustomField("Title", coreProps.title.toString(), coreProps.title != null)

    def extProps = doc.properties.extendedProperties
    if (extProps.application) {
        builder.addCustomField("Application", extProps.application)
    }
    if (extProps.appVersion) {
        builder.addCustomField("AppVersion", extProps.appVersion)
    }
}
