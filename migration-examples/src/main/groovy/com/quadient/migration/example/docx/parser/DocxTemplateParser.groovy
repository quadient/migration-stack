package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.PageOptions
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.PdfMetadataBuilder
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.shared.DocumentObjectType
import groovy.io.FileType
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable

import java.util.regex.Pattern

import static com.quadient.migration.example.docx.parser.DocxBlocks.blockName
import static com.quadient.migration.example.docx.parser.DocxBlocks.upsertBlock
import static com.quadient.migration.example.docx.parser.DocxHeadings.headingLevel
import static com.quadient.migration.example.docx.parser.DocxMergeFields.handleTableInsideField
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseFlowParagraph
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.warnUnterminatedField
import static com.quadient.migration.example.docx.parser.DocxStatistics.captureDocumentStatistics
import static com.quadient.migration.example.docx.parser.DocxStatistics.captureDocxFileProperties
import static com.quadient.migration.example.docx.parser.DocxTableParser.addRows
import static com.quadient.migration.example.docx.parser.DocxTableParser.createTableBuilder
import static com.quadient.migration.example.docx.parser.DocxTableParser.parseTable
import static com.quadient.migration.example.docx.parser.DocxTableParser.resolveExpectedColumnCount

static void parseDocxFiles(Migration migration) {
    List<File> inputFiles = []
    new File(migration.projectConfig.inputDataPath).eachFileRecurse(FileType.FILES) { File file ->
        if (file.name.endsWith('.docx') && !file.name.startsWith('~$')) {
            inputFiles.add(file)
        }
    }
    inputFiles.each { parseDocxFile(migration, it) }
}

static void parseDocxFile(Migration migration, File file) {
    String fileName = file.name.substring(0, file.name.lastIndexOf('.'))
    String documentType = file.parentFile.name
    String relativePath = new File(migration.projectConfig.inputDataPath).toPath().relativize(file.toPath()).toString()

    println("=== Processing: " + relativePath + " ===")
    DocumentObjectBuilder builder = new DocumentObjectBuilder(fileName, DocumentObjectType.Template)
            .name(fileName)
            .originLocations([relativePath])
            .targetFolder(resolveTargetFolder(migration, relativePath))
            .addCustomField("size", file.length().toString())
            .addCustomField("documentType", documentType)

    parsePages(migration, file, builder, fileName).each { builder.documentObjectRef(it) }

    DocumentObject documentObject = builder.build()
    migration.documentObjectRepository.upsert(documentObject)
}

private static String resolveTargetFolder(Migration migration, String relativePath) {
    String[] folders = relativePath.split(Pattern.quote(File.separator))
    String subfolder = folders.length > 1 ? folders[0..-2].join(File.separator) : ''
    String defaultTargetFolder = migration.projectConfig.defaultTargetFolder.toString()
    return [defaultTargetFolder, subfolder].findAll().join('/')
}

static List<DocumentObject> parsePages(Migration migration, File docxFile, DocumentObjectBuilder templateBuilder, String fileName) {
    XWPFDocument doc = new XWPFDocument(new FileInputStream(docxFile))
    try {
        captureDocxFileProperties(doc, templateBuilder)
        setPdfMetadata(templateBuilder, doc)
        DocxImages.resetImageState()

        List<DocxPage> pages = DocxPageSections.groupIntoPages(DocxPageSections.splitIntoSections(doc))
        DocxAnchoredAreas anchoredAreas = DocxAnchoredAreas.extract(migration, doc, fileName, pages)

        int otherElements = 0
        pages.each { DocxPage page ->
            String pageId = page.id(fileName)
            DocxBodyContent body = new DocxBodyContent(migration, fileName, pageId)
            FieldParseState fieldState = new FieldParseState(conditionalTableHandler: body.&addConditionalTable)
            int floatingTables = 0
            page.bodyElements().each { elem ->
                if (elem instanceof XWPFParagraph) {
                    Paragraph paragraph = parseFlowParagraph(migration, elem, fileName, fieldState, null, anchoredAreas.consumedEmbedIds)
                    if (paragraph != null) {
                        body.add(paragraph, headingLevel(elem))
                    }
                } else if (elem instanceof XWPFTable) {
                    if (fieldState.depth > 0) {
                        // The table sits inside an open IF field; it is emitted (with a display rule) once the field resolves.
                        handleTableInsideField(fieldState, elem)
                    } else if (DocxFloatingTables.isFloating(elem)) {
                        Area area = buildFloatingTableArea(migration, elem, page, fileName, "${pageId}_floating_table${++floatingTables}")
                        if (area != null) {
                            anchoredAreas.floatingAreasByPage.computeIfAbsent(page.index) { [] }.add(area)
                        }
                    } else {
                        addBodyTable(migration, body, elem, fileName)
                    }
                } else {
                    otherElements++
                }
            }
            warnUnterminatedField(fieldState, "page ${page.index + 1} body")
            page.content = body.sectionBlocks()
        }
        if (otherElements > 0) {
            templateBuilder.addCustomField("otherElements", otherElements.toString())
        }

        captureDocumentStatistics(doc, templateBuilder)

        return pages.collect { buildPage(migration, doc, fileName, it, anchoredAreas) }
    } finally {
        doc.close()
    }
}

// A floating table is positioned absolutely, so it becomes an area of its own whose content is kept in a separate block.
private static Area buildFloatingTableArea(Migration migration, XWPFTable table, DocxPage page, String fileName, String blockId) {
    Table parsedTable = parseTable(migration, table, fileName)
    String firstCellText = table.rows[0]?.tableCells?.collect { it.text.trim() }?.find { it }
    DocumentObjectRef blockRef = upsertBlock(migration, blockId, blockName(firstCellText, "table", blockId), [parsedTable], fileName)
    return DocxFloatingTables.buildFloatingTableArea(table, [blockRef], page.contentPosition)
}

private static void addBodyTable(Migration migration, DocxBodyContent body, XWPFTable table, String fileName) {
    int columnCount = resolveExpectedColumnCount(table)
    TableBuilder tableBuilder = createTableBuilder(migration, table, true)
    addRows(migration, tableBuilder, table, table.rows, fileName, columnCount, true)
    body.addTable(table, tableBuilder, columnCount)
}

private static DocumentObject buildPage(Migration migration, XWPFDocument doc, String fileName, DocxPage page, DocxAnchoredAreas anchoredAreas) {
    Area mainFlowArea = new AreaBuilder()
            .interactiveFlowName("Letter Content")
            .content(page.content)
            .position(page.contentPosition)
            .flowToNextPage(true)
            .build()

    int pageNumber = page.index + 1
    DocumentObjectBuilder pageBuilder = new DocumentObjectBuilder(page.id(fileName), DocumentObjectType.Page)
            .internal(true)
            .name("${fileName} Page ${pageNumber}")
            .originLocations([fileName])
            .options(new PageOptions(page.width, page.height))
    anchoredAreas.backgroundAreasByPage[page.index]?.each { pageBuilder.area(it) }
    DocxHeaderFooters.headerAreas(migration, doc, page, fileName).each { pageBuilder.area(it) }
    pageBuilder.area(mainFlowArea)
    DocxHeaderFooters.footerAreas(migration, doc, page, fileName).each { pageBuilder.area(it) }
    anchoredAreas.floatingAreasByPage[page.index]?.each { pageBuilder.area(it) }

    DocumentObject pageObject = pageBuilder.build()
    migration.documentObjectRepository.upsert(pageObject)
    return pageObject
}

// The PDF metadata of the output is taken from the document properties of the DOCX file.
static void setPdfMetadata(DocumentObjectBuilder builder, XWPFDocument doc) {
    def coreProps = doc.properties.coreProperties
    String title = coreProps.title?.trim()
    String author = coreProps.creator?.trim()
    String subject = coreProps.subject?.trim()
    if (!title && !author && !subject) {
        return
    }
    PdfMetadataBuilder pdfMetadata = new PdfMetadataBuilder()
    if (title) pdfMetadata.title(title)
    if (author) pdfMetadata.author(author)
    if (subject) pdfMetadata.subject(subject)
    builder.setPdfMetadata(pdfMetadata.build())
}
