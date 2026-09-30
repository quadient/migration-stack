package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.Size
import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.xmlbeans.XmlCursor
import org.apache.xmlbeans.XmlObject

import javax.xml.namespace.QName
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

@Field
private static final String VML_NS = "urn:schemas-microsoft-com:vml"
@Field
private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

class VmlImageSource {
    String embedId
    byte[] bytes
    ImageType imageType
    ImageOptions options
    Long checksum
}

static boolean hasImages(XWPFRun run) {
    return !findShapes(run).isEmpty()
}

static boolean hasAbsolutelyPositionedImage(XWPFParagraph paragraph) {
    return paragraph.runs.any { XWPFRun run ->
        findShapes(run).any { XmlObject shape ->
            String style = attribute(shape, "style")
            style && (style =~ /(?i)(?:^|;)\s*position\s*:\s*absolute\s*(?:;|$)/).find()
        }
    }
}

static List<VmlImageSource> extract(XWPFRun run) {
    return findShapes(run).findResults { XmlObject shape -> toImageSource(shape) }
}

private static VmlImageSource toImageSource(XmlObject shape) {
    ImageOptions options = resolveOptions(attribute(shape, "style"))
    XmlObject[] imageData = shape.selectPath("declare namespace v='${VML_NS}' .//v:imagedata")
    if (imageData.length > 0) {
        String embedId = attribute(imageData[0], "id", REL_NS)
        return embedId ? new VmlImageSource(embedId: embedId, options: options) : null
    }

    String svg = shapeToSvg(shape)
    if (svg == null) {
        return null
    }
    byte[] bytes = svg.getBytes(StandardCharsets.UTF_8)
    CRC32 crc = new CRC32()
    crc.update(bytes)
    return new VmlImageSource(bytes: bytes, imageType: ImageType.Svg, options: options, checksum: crc.value)
}

private static List<XmlObject> findShapes(XWPFRun run) {
    return run.CTR.selectPath("declare namespace v='${VML_NS}' .//v:shape").toList()
}

private static ImageOptions resolveOptions(String style) {
    Double width = styleSizeInPoints(style, "width")
    Double height = styleSizeInPoints(style, "height")
    return width != null && height != null && width > 0 && height > 0
            ? new ImageOptions(Size.ofPoints(width), Size.ofPoints(height))
            : null
}

private static Double styleSizeInPoints(String style, String property) {
    if (!style) {
        return null
    }
    def matcher = style =~ /(?i)(?:^|;)\s*${property}\s*:\s*([+-]?(?:\d+(?:\.\d*)?|\.\d+))\s*(pt|px|in|cm|mm)?\s*(?:;|$)/
    if (!matcher.find()) {
        return null
    }
    double value = matcher.group(1) as double
    switch ((matcher.group(2) ?: "pt").toLowerCase()) {
        case "px": return value * 0.75d
        case "in": return value * 72.0d
        case "cm": return value * 72.0d / 2.54d
        case "mm": return value * 72.0d / 25.4d
        default: return value
    }
}

private static String shapeToSvg(XmlObject shape) {
    String path = vmlPathToSvgPath(attribute(shape, "path"))
    List<Double> coordSize = coordinatePair(attribute(shape, "coordsize"))
    if (!path || coordSize == null || coordSize.any { it <= 0 }) {
        return null
    }
    List<Double> coordOrigin = coordinatePair(attribute(shape, "coordorigin")) ?: [0.0d, 0.0d]
    String fill = attribute(shape, "filled") == "f" ? "none" : safeColor(attribute(shape, "fillcolor"), "white")
    String stroke = attribute(shape, "stroked") == "f" ? "none" : safeColor(attribute(shape, "strokecolor"), "black")
    return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"${number(coordOrigin[0])} ${number(coordOrigin[1])} ${number(coordSize[0])} ${number(coordSize[1])}\"><path d=\"${path}\" fill=\"${fill}\" stroke=\"${stroke}\"/></svg>"
}

static String vmlPathToSvgPath(String vmlPath) {
    if (!vmlPath) {
        return null
    }
    StringBuilder svg = new StringBuilder()
    int index = 0
    while (index < vmlPath.length()) {
        while (index < vmlPath.length() && (Character.isWhitespace(vmlPath.charAt(index)) || vmlPath.charAt(index) == ',')) index++
        if (index >= vmlPath.length()) break
        if (!Character.isLetter(vmlPath.charAt(index))) return null
        String command = Character.toString(vmlPath.charAt(index++)).toLowerCase()
        int argsStart = index
        while (index < vmlPath.length() && !Character.isLetter(vmlPath.charAt(index))) index++
        List<Double> args = coordinates(vmlPath.substring(argsStart, index))

        switch (command) {
            case "m":
            case "l":
            case "t":
            case "r":
                if (!appendPairs(svg, command == "m" ? "M" : command == "l" ? "L" : command == "t" ? "m" : "l", args, command == "m")) return null
                break
            case "c":
            case "v":
                if (!appendGroups(svg, command == "c" ? "C" : "c", args, 6)) return null
                break
            case "x":
                appendCommand(svg, "Z", [])
                break
            case "e":
                return svg.toString()
            default:
                return null
        }
    }
    return svg.toString()
}

private static boolean appendPairs(StringBuilder svg, String command, List<Double> args, boolean move) {
    if (args.isEmpty() || args.size() % 2 != 0) return false
    args.collate(2).eachWithIndex { List<Double> pair, int i ->
        appendCommand(svg, move && i > 0 ? "L" : command, pair)
    }
    return true
}

private static boolean appendGroups(StringBuilder svg, String command, List<Double> args, int groupSize) {
    if (args.isEmpty() || args.size() % groupSize != 0) return false
    args.collate(groupSize).each { appendCommand(svg, command, it) }
    return true
}

private static void appendCommand(StringBuilder svg, String command, List<Double> args) {
    if (svg.length() > 0) svg.append(' ')
    svg.append(command)
    if (!args.isEmpty()) svg.append(' ').append(args.collect { number(it) }.join(' '))
}

private static List<Double> coordinates(String text) {
    String normalized = text.trim().replaceAll(/\s+/, ',')
    if (!normalized) return []
    return normalized.split(',', -1).collect { it ? it as double : 0.0d }
}

private static List<Double> coordinatePair(String text) {
    if (!text) return null
    List<Double> values = coordinates(text)
    return values.size() == 2 ? values : null
}

private static String safeColor(String value, String fallback) {
    return value ==~ /(?i)(?:#[0-9a-f]{3,8}|[a-z]+|none)/ ? value : fallback
}

private static String number(double value) {
    return value == Math.rint(value) ? Long.toString(value as long) : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

private static String attribute(XmlObject object, String localName, String namespace = "") {
    XmlCursor cursor = object.newCursor()
    try {
        return cursor.getAttributeText(new QName(namespace, localName))
    } finally {
        cursor.dispose()
    }
}
