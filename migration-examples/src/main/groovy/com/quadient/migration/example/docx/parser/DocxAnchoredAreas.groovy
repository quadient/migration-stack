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
import org.w3c.dom.Element
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
    private static final String VML_NS = "urn:schemas-microsoft-com:vml"
    private static final String MC_NS = "http://schemas.openxmlformats.org/markup-compatibility/2006"
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
        pages.each { DocxPage page ->
            result.eachUniqueAnchor(page) { result.addFloatingArea(it, page) }
            result.eachUniqueVmlTextBox(page) { result.addVmlTextBoxArea(it, page) }
        }
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

    // Older Word documents use VML w:pict/v:shape text boxes rather than DrawingML wp:anchor shapes.
    private void eachUniqueVmlTextBox(DocxPage page, Closure<Void> handler) {
        Set<Object> seenShapeIds = []
        page.paragraphs().each { XWPFParagraph paragraph ->
            paragraph.runs.each { XWPFRun run ->
                findVmlTextBoxes(run).each { Element shape ->
                    String shapeId = vmlShapeId(shape)
                    if (seenShapeIds.add(shapeId ?: shape.textContent)) {
                        handler(shape)
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

    private void addVmlTextBoxArea(Element shape, DocxPage page) {
        Area area = buildVmlTextBoxArea(shape, page)
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

    private List<Element> findVmlTextBoxes(XWPFRun run) {
        // XMLBeans' XPath support can require optional Saxon classes.  DOM traversal keeps this parser self-contained.
        Element runDom = documentBuilder.parse(new InputSource(new StringReader(run.CTR.xmlText()))).documentElement
        def shapes = runDom.getElementsByTagNameNS(VML_NS, 'shape')
        return (0..<shapes.length).collect { shapes.item(it) as Element }
                .findAll { it.getElementsByTagNameNS(W_NS, 'txbxContent').length > 0 && !isVmlFallbackForAnchor(it) }
    }

    // Word's AlternateContent stores the same floating text box twice: a modern wp:anchor for current clients and
    // a VML shape fallback for older ones.  POI exposes both branches, but only the modern choice is rendered.
    private static boolean isVmlFallbackForAnchor(Element shape) {
        Node current = shape
        while ((current = current.parentNode) != null) {
            if (current.nodeType != Node.ELEMENT_NODE || current.namespaceURI != MC_NS || current.localName != 'Fallback') {
                continue
            }
            Node alternateContent = current.parentNode
            return alternateContent instanceof Element && alternateContent.namespaceURI == MC_NS &&
                    alternateContent.localName == 'AlternateContent' &&
                    (alternateContent as Element).getElementsByTagNameNS(WP_NS, 'anchor').length > 0
        }
        return false
    }

    private static String vmlShapeId(Element shape) {
        return shape.getAttribute('id') ?: null
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
            return buildTextBoxArea(cursor.xmlText(), resolveAnchorPosition(anchor, page), page)
        } finally {
            cursor.dispose()
        }
    }

    private Area buildVmlTextBoxArea(Element shape, DocxPage page) {
        return buildTextBoxArea(shape, resolveVmlShapePosition(shape, page), page)
    }

    private Area buildTextBoxArea(String shapeXml, Position position, DocxPage page) {
        def shapeDom = documentBuilder.parse(new InputSource(new StringReader(shapeXml)))
        return buildTextBoxArea(shapeDom.documentElement, position, page)
    }

    private Area buildTextBoxArea(Element shapeDom, Position position, DocxPage page) {
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
        return new AreaBuilder().content([blockRef]).position(position).build()
    }

    private Position resolveVmlShapePosition(Element shape, DocxPage page) {
        Map<String, String> style = vmlStyle(shape.getAttribute('style'))
        double x = page.contentPosition.x.toPoints() + vmlPoints(style['margin-left'])
        double y = page.contentPosition.y.toPoints() + vmlPoints(style['margin-top'])
        return new Position(Size.ofPoints(x), Size.ofPoints(y), Size.ofPoints(vmlPoints(style['width'])), Size.ofPoints(vmlPoints(style['height'])))
    }

    private static Map<String, String> vmlStyle(String value) {
        return value.split(';').collectEntries { String property ->
            int separator = property.indexOf(':')
            separator < 0 ? [:] : [(property.substring(0, separator).trim().toLowerCase(Locale.ROOT)): property.substring(separator + 1).trim()]
        }
    }

    // Word's VML geometry is CSS-like; point values dominate its generated documents, with the common alternatives
    // handled here as well so that the resulting Area geometry stays in points.
    private static double vmlPoints(String value) {
        def matcher = value =~ /^([+-]?(?:\d+(?:\.\d*)?|\.\d+))(pt|in|cm|mm|px)?$/
        if (!matcher.matches()) {
            return 0d
        }
        double number = matcher.group(1) as double
        return switch (matcher.group(2)?.toLowerCase(Locale.ROOT)) {
            case 'in' -> number * 72d
            case 'cm' -> number * 72d / 2.54d
            case 'mm' -> number * 72d / 25.4d
            case 'px' -> number * 72d / 96d
            default -> number
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
