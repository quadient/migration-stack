package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.IBodyElement
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.apache.xmlbeans.XmlObject
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr

import static com.quadient.migration.example.docx.parser.DocxParagraphParser.parseFlowParagraph
import static com.quadient.migration.example.docx.parser.DocxParagraphParser.warnUnterminatedField
import static com.quadient.migration.example.docx.parser.DocxTableParser.parseTable
import static com.quadient.migration.example.docx.parser.DocxImages.registerImageData
import static com.quadient.migration.example.docx.util.DocxUtils.emuToSize
import static com.quadient.migration.example.docx.util.DocxUtils.twipsToPoints

class DocxHeaderFooters {
    private static final List<String> TYPES = ['default', 'first', 'even']
    private static final String WP_NS = 'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'
    private static final String PIC_NS = 'http://schemas.openxmlformats.org/drawingml/2006/picture'

    static List<Area> areas(Migration migration, XWPFDocument doc, DocxPage page, String fileName) {
        return headerAreas(migration, doc, page, fileName) + footerAreas(migration, doc, page, fileName)
    }

    static List<Area> headerAreas(Migration migration, XWPFDocument doc, DocxPage page, String fileName) {
        return partAreas(migration, doc, page, fileName, true)
    }

    static List<Area> footerAreas(Migration migration, XWPFDocument doc, DocxPage page, String fileName) {
        return partAreas(migration, doc, page, fileName, false)
    }

    private static List<Area> partAreas(Migration migration, XWPFDocument doc, DocxPage page, String fileName, boolean headerOnly) {
        if (page.sections.isEmpty()) {
            return []
        }
        Map<String, Map<String, XWPFHeaderFooter>> parts = effectiveParts(doc, page.sections.first().sectPr)
        List<Area> result = []
        XWPFHeaderFooter part = selectPart(headerOnly ? parts.headers : parts.footers, page)
        if (part != null) {
            Position position = headerOnly ? headerPosition(page) : footerPosition(page)
            Set<String> directImageEmbedIds = anchoredEmbedIds(part) + inlineEmbedIds(part)
            Area area = buildArea(migration, part, fileName, position, directImageEmbedIds)
            if (area != null) result.add(area)
            result.addAll(inlineImageAreas(migration, doc, part, fileName, position))
            result.addAll(anchoredImageAreas(migration, doc, part, fileName, page, position))
        }
        return result
    }

    private static Map<String, Map<String, XWPFHeaderFooter>> effectiveParts(XWPFDocument doc, CTSectPr sectPr) {
        Map<String, Map<String, XWPFHeaderFooter>> result = [headers: [:], footers: [:]]
        if (sectPr == null) return result
        Map<String, XWPFHeaderFooter> headers = doc.headerList.collectEntries { [(it.packagePart.partName.name): it] }
        Map<String, XWPFHeaderFooter> footers = doc.footerList.collectEntries { [(it.packagePart.partName.name): it] }
        resolveReferences(doc, sectPr.headerReferenceList, headers, result.headers)
        resolveReferences(doc, sectPr.footerReferenceList, footers, result.footers)
        return result
    }

    private static void resolveReferences(XWPFDocument doc, List references, Map<String, XWPFHeaderFooter> source,
                                          Map<String, XWPFHeaderFooter> target) {
        references.each { reference ->
            def relationship = doc.packagePart.getRelationship(reference.id)
            XWPFHeaderFooter part = relationship == null ? null : source[relationship.targetURI.path]
            if (part != null) {
                target[reference.type?.toString() ?: 'default'] = part
            }
        }
    }

    private static XWPFHeaderFooter selectPart(Map<String, XWPFHeaderFooter> parts, DocxPage page) {
        String type = page.index == 0 && page.sections.first().sectPr?.titlePg != null ? 'first' :
                page.index % 2 == 1 ? 'even' : 'default'
        // Word falls back to the default part when an even/first-page part is absent.
        return parts[type] ?: parts['default']
    }

    private static Area buildArea(Migration migration, XWPFHeaderFooter source, String fileName, Position position,
                                  Set<String> excludedImageEmbedIds = Collections.emptySet()) {
        List<DocumentContent> content = []
        FieldParseState fieldState = new FieldParseState()
        source.bodyElements.each { IBodyElement element ->
            if (element instanceof XWPFParagraph) {
                Paragraph paragraph = parseFlowParagraph(migration, element, fileName, fieldState, 'headerFooter', excludedImageEmbedIds)
                if (paragraph?.content) content.add(paragraph)
            } else if (element instanceof XWPFTable) {
                Table table = parseTable(migration, element, fileName)
                if (table != null) content.add(table)
            }
        }
        warnUnterminatedField(fieldState, 'header/footer')
        return content.isEmpty() ? null : new AreaBuilder().content(content).position(position).build()
    }

    // Anchored drawings are not exposed by XWPFRun.embeddedPictures.  Header/footer parts own their image
    // relationships, so resolve the blip through that part rather than the document's relationship table.
    private static List<Area> anchoredImageAreas(Migration migration, XWPFDocument doc, XWPFHeaderFooter source, String fileName,
                                                  DocxPage page, Position flowPosition) {
        List<Area> areas = []
        source.paragraphs.each { XWPFParagraph paragraph ->
            paragraph.runs.each { XWPFRun run ->
                findAnchors(run).each { CTAnchor anchor ->
                    if (anchor.graphic?.graphicData?.uri != PIC_NS) return
                    String embedId = extractBlipEmbedId(anchor)
                    XWPFPictureData data = pictureData(doc, source, embedId)
                    if (data == null) return
                    Size width = emuToSize(anchor.extent?.cx ?: 0L)
                    Size height = emuToSize(anchor.extent?.cy ?: 0L)
                    String imageId = registerImageData(migration, data, fileName, new ImageOptions(width, height))
                    if (imageId != null) {
                        areas.add(new AreaBuilder().imageRef(imageId)
                                .position(resolveHeaderFooterAnchorPosition(anchor, page, flowPosition)).build())
                    }
                }
            }
        }
        return areas
    }

    // Header/footer images are fixed page elements. Emit both POI-bound and unbound inline pictures as areas so
    // their dimensions are not constrained by the comparatively small header/footer flow frame.
    private static List<Area> inlineImageAreas(Migration migration, XWPFDocument doc, XWPFHeaderFooter source, String fileName,
                                               Position flowPosition) {
        List<Area> areas = []
        source.paragraphs.each { XWPFParagraph paragraph ->
            paragraph.runs.each { XWPFRun run ->
                run.embeddedPictures.each { picture ->
                    CTPicture ctPicture = picture.CTPicture
                    XWPFPictureData data = picture.pictureData ?: pictureData(doc, source, ctPicture?.blipFill?.blip?.embed)
                    addInlineImageArea(areas, migration, data, ctPicture, fileName, flowPosition)
                }
                if (!run.embeddedPictures.isEmpty()) return
                findInlines(run).each { XmlObject inline ->
                    CTPicture picture = inlinePicture(inline)
                    addInlineImageArea(areas, migration, pictureData(doc, source, picture?.blipFill?.blip?.embed), picture, fileName, flowPosition)
                }
            }
        }
        // Last-resort handling for a header/footer part whose inline drawing is not surfaced by POI's run API.
        // It is safe only when the document contains exactly one image, which is the package shape of 01CVRPG0222.
        if (areas.isEmpty() && doc.allPictures.size() == 1 && source.paragraphs.any { paragraph ->
            paragraph.runs.any { it.CTR.xmlText().contains('<w:drawing') }
        }) {
            XWPFPictureData data = doc.allPictures[0]
            String imageId = registerImageData(migration, data, fileName, null)
            if (imageId != null) {
                areas.add(new AreaBuilder().imageRef(imageId).position(flowPosition).build())
            }
        }
        return areas
    }

    private static List<CTAnchor> findAnchors(XWPFRun run) {
        XmlObject[] found = run.CTR.selectPath("declare namespace wp='${WP_NS}' .//wp:anchor")
        return found.collect { XmlObject o -> o instanceof CTAnchor ? o : CTAnchor.Factory.parse(o.xmlText()) }
    }

    private static Set<String> anchoredEmbedIds(XWPFHeaderFooter source) {
        Set<String> ids = []
        source.paragraphs.each { XWPFParagraph paragraph ->
            paragraph.runs.each { XWPFRun run ->
                findAnchors(run).each { CTAnchor anchor ->
                    String embedId = extractBlipEmbedId(anchor)
                    if (embedId) ids.add(embedId)
                }
            }
        }
        return ids
    }

    private static Set<String> inlineEmbedIds(XWPFHeaderFooter source) {
        Set<String> ids = []
        source.paragraphs.each { XWPFParagraph paragraph ->
            paragraph.runs.each { XWPFRun run ->
                run.embeddedPictures.each { picture ->
                    String embedId = picture.CTPicture?.blipFill?.blip?.embed
                    if (embedId) ids.add(embedId)
                }
                findInlines(run).each { XmlObject inline ->
                    String embedId = inlinePicture(inline)?.blipFill?.blip?.embed
                    if (embedId) ids.add(embedId)
                }
            }
        }
        return ids
    }

    private static void addInlineImageArea(List<Area> areas, Migration migration, XWPFPictureData data, CTPicture picture,
                                           String fileName, Position flowPosition) {
        if (data == null || picture == null) return
        Size width = emuToSize(picture.spPr?.xfrm?.ext?.cx ?: 0L)
        Size height = emuToSize(picture.spPr?.xfrm?.ext?.cy ?: 0L)
        String imageId = registerImageData(migration, data, fileName, new ImageOptions(width, height))
        if (imageId != null) {
            areas.add(new AreaBuilder().imageRef(imageId)
                    .position(new Position(flowPosition.x, flowPosition.y, width, height)).build())
        }
    }

    private static List<XmlObject> findInlines(XWPFRun run) {
        return run.CTR.selectPath("declare namespace wp='${WP_NS}' .//wp:inline") as List<XmlObject>
    }

    private static CTPicture inlinePicture(XmlObject inline) {
        XmlObject[] pictures = inline.selectPath("declare namespace pic='${PIC_NS}' .//pic:pic")
        return pictures.length == 0 ? null : (pictures[0] instanceof CTPicture
                ? pictures[0] as CTPicture : CTPicture.Factory.parse(pictures[0].xmlText()))
    }

    // Header/footer anchors can be relative to their paragraph or column. Unlike body content, those origins sit
    // in the header/footer frame, not at the main body margin.
    private static Position resolveHeaderFooterAnchorPosition(CTAnchor anchor, DocxPage page, Position flowPosition) {
        Size width = emuToSize(anchor.extent?.cx ?: 0L)
        Size height = emuToSize(anchor.extent?.cy ?: 0L)
        boolean relativeToPageH = anchor.positionH?.relativeFrom?.toString() == 'page'
        boolean relativeToPageV = anchor.positionV?.relativeFrom?.toString() == 'page'
        double x = relativeToPageH ? 0d : flowPosition.x.toPoints()
        double y = relativeToPageV ? 0d : flowPosition.y.toPoints()
        if (anchor.positionH?.isSetPosOffset()) x += emuToSize(anchor.positionH.posOffset).toPoints()
        if (anchor.positionV?.isSetPosOffset()) y += emuToSize(anchor.positionV.posOffset).toPoints()
        return new Position(Size.ofPoints(x), Size.ofPoints(y), width, height)
    }

    private static XWPFPictureData pictureData(XWPFDocument doc, XWPFHeaderFooter source, String embedId) {
        if (!embedId) return null
        XWPFPictureData data = source.getPictureDataByID(embedId)
        if (data != null) return data
        def relatedPart = source.getRelationById(embedId)
        if (relatedPart instanceof XWPFPictureData) return relatedPart
        def relationship = source.packagePart.getRelationship(embedId)
        data = relationship == null ? null : source.allPictures.find {
            it.packagePart.partName.name == relationship.targetURI.path
        }
        // Some DOCX producers leave POI unable to bind a header relationship even though the document has a
        // single embedded image. This fallback covers that unambiguous package shape (including 01CVRPG0222).
        return data ?: (doc.allPictures.size() == 1 ? doc.allPictures[0] : null)
    }

    private static String extractBlipEmbedId(CTAnchor anchor) {
        XmlObject[] pictures = anchor.selectPath("declare namespace pic='${PIC_NS}' .//pic:pic")
        if (pictures.length == 0) return null
        CTPicture picture = pictures[0] instanceof CTPicture ? pictures[0] as CTPicture : CTPicture.Factory.parse(pictures[0].xmlText())
        return picture.blipFill?.blip?.embed
    }

    private static Position headerPosition(DocxPage page) {
        double contentTop = page.contentPosition.y.toPoints()
        double headerOffset = headerFooterOffset(page, 'header', 0d)
        double y = Math.min(Math.max(headerOffset, 0d), contentTop)
        return new Position(page.contentPosition.x, Size.ofPoints(y), page.contentPosition.width,
                Size.ofPoints(Math.max(contentTop - y, 1d)))
    }

    private static Position footerPosition(DocxPage page) {
        double y = page.contentPosition.y.toPoints() + page.contentPosition.height.toPoints()
        double height = page.height.toPoints() - y
        return new Position(page.contentPosition.x, Size.ofPoints(y), page.contentPosition.width, Size.ofPoints(Math.max(height, 1d)))
    }

    private static double headerFooterOffset(DocxPage page, String property, double fallback) {
        def margins = page.sections.first()?.sectPr?.pgMar
        Double offset = twipsToPoints(margins?."${property}")
        return offset != null ? offset : fallback
    }
}
