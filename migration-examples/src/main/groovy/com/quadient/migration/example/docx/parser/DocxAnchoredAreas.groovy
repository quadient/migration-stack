package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.xmlbeans.XmlCursor
import org.apache.xmlbeans.XmlObject
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP
import org.w3c.dom.Node
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import static com.quadient.migration.example.docx.parser.DocxBlocks.blockName
import static com.quadient.migration.example.docx.parser.DocxBlocks.upsertBlock
import static com.quadient.migration.example.docx.parser.DocxImages.registerImageData
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseParagraph
import static com.quadient.migration.example.docx.util.DocxUtils.emuToSize
import static com.quadient.migration.example.docx.util.DocxUtils.extractParagraphText

class DocxAnchoredAreas {
    private static final String WP_NS = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private static final String WPS_SHAPE_URI = "http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
    private static final String PIC_NS = "http://schemas.openxmlformats.org/drawingml/2006/picture"
    private static final double BACKGROUND_COVERAGE_THRESHOLD = 0.6

    final Map<Integer, List<Area>> backgroundAreasByPage = [:]
    final Map<Integer, List<Area>> floatingAreasByPage = [:]
    final Set<String> consumedEmbedIds = []
    private final Set<String> backgroundEmbedIds = []
    private int textBoxes = 0

    private final Migration migration
    private final XWPFDocument doc
    private final String fileName
    private final DocumentBuilder documentBuilder
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance()

    private DocxAnchoredAreas(Migration migration, XWPFDocument doc, String fileName) {
        this.migration = migration
        this.doc = doc
        this.fileName = fileName
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance()
        dbf.namespaceAware = true
        this.documentBuilder = dbf.newDocumentBuilder()
    }

    static DocxAnchoredAreas extract(Migration migration, XWPFDocument doc, String fileName, List<DocxPage> pages) {
        DocxAnchoredAreas result = new DocxAnchoredAreas(migration, doc, fileName)
        // Backgrounds are resolved over the whole document first so the same picture is never emitted as floating too.
        pages.each { DocxPage page -> result.eachUniqueAnchor(page) { result.addBackgroundArea(it, page) } }
        pages.each { DocxPage page -> result.eachUniqueAnchor(page) { result.addFloatingArea(it, page) } }
        return result
    }

    private void eachUniqueAnchor(DocxPage page, Closure<Void> handler) {
        Set<Object> seenDrawingIds = []
        page.paragraphs().each { XWPFParagraph paragraph ->
            paragraph.runs.each { XWPFRun run ->
                findAnchors(run).each { CTAnchor anchor ->
                    if (seenDrawingIds.add(anchor.docPr?.id ?: anchor.xmlText())) {
                        handler(anchor)
                    }
                }
            }
        }
    }

    private void addBackgroundArea(CTAnchor anchor, DocxPage page) {
        if (!isPicture(anchor) || !coversPage(anchor, page)) {
            return
        }
        String embedId = extractBlipEmbedId(anchor)
        Position position = new Position(Size.ofPoints(0), Size.ofPoints(0), page.width, page.height)
        Area area = embedId ? buildImageArea(anchor, embedId, position) : null
        if (area != null) {
            backgroundAreasByPage.computeIfAbsent(page.index) { [] }.add(area)
            backgroundEmbedIds.add(embedId)
            consumedEmbedIds.add(embedId)
        }
    }

    private void addFloatingArea(CTAnchor anchor, DocxPage page) {
        Area area = null
        if (anchor.graphic?.graphicData?.uri == WPS_SHAPE_URI) {
            area = buildTextBoxArea(anchor, page)
        } else if (isPicture(anchor)) {
            String embedId = extractBlipEmbedId(anchor)
            if (embedId && !backgroundEmbedIds.contains(embedId)) {
                area = buildImageArea(anchor, embedId, resolveAnchorPosition(anchor, page))
                if (area != null) {
                    consumedEmbedIds.add(embedId)
                }
            }
        }
        if (area != null) {
            floatingAreasByPage.computeIfAbsent(page.index) { [] }.add(area)
        }
    }

    private static boolean isPicture(CTAnchor anchor) {
        return anchor.graphic?.graphicData?.uri == PIC_NS
    }

    private static boolean coversPage(CTAnchor anchor, DocxPage page) {
        Size width = emuToSize(anchor.extent?.cx ?: 0L)
        Size height = emuToSize(anchor.extent?.cy ?: 0L)
        return width.toPoints() >= page.width.toPoints() * BACKGROUND_COVERAGE_THRESHOLD
                && height.toPoints() >= page.height.toPoints() * BACKGROUND_COVERAGE_THRESHOLD
    }

    private static List<CTAnchor> findAnchors(XWPFRun run) {
        XmlObject[] found = run.CTR.selectPath("declare namespace wp='${WP_NS}' .//wp:anchor")
        return found.collect { XmlObject o -> o instanceof CTAnchor ? o : CTAnchor.Factory.parse(o.xmlText()) }
    }

    private static String extractBlipEmbedId(CTAnchor anchor) {
        XmlObject[] pics = anchor.selectPath("declare namespace pic='${PIC_NS}' .//pic:pic")
        if (pics.length == 0) {
            return null
        }
        CTPicture pic = pics[0] instanceof CTPicture ? pics[0] as CTPicture : CTPicture.Factory.parse(pics[0].toString())
        return pic.blipFill?.blip?.embed
    }

    private Area buildImageArea(CTAnchor anchor, String embedId, Position position) {
        XWPFPictureData data = doc.getPictureDataByID(embedId)
        if (data == null) {
            return null
        }
        Size width = emuToSize(anchor.extent?.cx ?: 0L)
        Size height = emuToSize(anchor.extent?.cy ?: 0L)
        String imageId = registerImageData(migration, data, fileName, new ImageOptions(width, height))
        if (imageId == null) {
            return null
        }
        return new AreaBuilder().imageRef(imageId).position(position).build()
    }

    private Area buildTextBoxArea(CTAnchor anchor, DocxPage page) {
        XmlCursor cursor = anchor.graphic.graphicData.newCursor()
        try {
            if (!cursor.toFirstChild()) {
                return null
            }
            def shapeDom = documentBuilder.parse(new InputSource(new StringReader(cursor.xmlText())))
            def txbxContentNodes = shapeDom.getElementsByTagNameNS(W_NS, "txbxContent")
            if (txbxContentNodes.length == 0) {
                return null
            }
            List<DocumentContent> contentItems = []
            def children = txbxContentNodes.item(0).childNodes
            for (int i = 0; i < children.length; i++) {
                Node child = children.item(i)
                if (child.nodeType == Node.ELEMENT_NODE && child.localName == 'p') {
                    XWPFParagraph paragraph = new XWPFParagraph(domParagraphToCtp(child), doc)
                    contentItems.add(parseParagraph(migration, paragraph, fileName))
                }
            }
            if (contentItems.isEmpty()) {
                return null
            }
            String blockId = "${page.id(fileName)}_textbox${++textBoxes}"
            String firstText = contentItems.findResult { extractParagraphText(it)?.trim() ?: null }
            DocumentObjectRef blockRef = upsertBlock(migration, blockId, blockName(firstText, "text box", blockId), contentItems, fileName)
            return new AreaBuilder().content([blockRef]).position(resolveAnchorPosition(anchor, page)).build()
        } finally {
            cursor.dispose()
        }
    }

    private CTP domParagraphToCtp(Node pNode) {
        StringWriter sw = new StringWriter()
        Transformer transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.transform(new DOMSource(pNode), new StreamResult(sw))
        String fragmentXml = sw.toString().replaceFirst(/^<w:p /, '<xml-fragment ').replaceFirst(/<\/w:p>$/, '</xml-fragment>')
        return CTP.Factory.parse(fragmentXml)
    }

    static Position resolveAnchorPosition(CTAnchor anchor, DocxPage page) {
        Size extentWidth = emuToSize(anchor.extent?.cx ?: 0L)
        Size extentHeight = emuToSize(anchor.extent?.cy ?: 0L)
        double marginLeft = page.contentPosition.x.toPoints()
        double marginTop = page.contentPosition.y.toPoints()
        boolean relativeToPageH = anchor.positionH?.relativeFrom?.toString() == "page"
        boolean relativeToPageV = anchor.positionV?.relativeFrom?.toString() == "page"

        double x
        if (anchor.positionH?.isSetPosOffset()) {
            x = emuToSize(anchor.positionH.posOffset).toPoints() + (relativeToPageH ? 0.0d : marginLeft)
        } else {
            double leftBound = relativeToPageH ? 0.0d : marginLeft
            double rightBound = relativeToPageH ? page.width.toPoints() : marginLeft + page.contentPosition.width.toPoints()
            switch (anchor.positionH?.align?.toString()) {
                case "right":
                case "outside":
                    x = rightBound - extentWidth.toPoints()
                    break
                case "center":
                    x = leftBound + (rightBound - leftBound - extentWidth.toPoints()) / 2.0d
                    break
                default:
                    x = leftBound
            }
        }

        double y = anchor.positionV?.isSetPosOffset()
                ? emuToSize(anchor.positionV.posOffset).toPoints() + (relativeToPageV ? 0.0d : marginTop)
                : marginTop

        return new Position(Size.ofPoints(x), Size.ofPoints(y), extentWidth, extentHeight)
    }
}
