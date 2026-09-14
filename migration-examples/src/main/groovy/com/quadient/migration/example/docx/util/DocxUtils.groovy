package com.quadient.migration.example.docx.util

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.shared.Size
import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFTableCell

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Field
static final double EMU_PER_POINT = 12700.0

static Double twipsToPoints(Object twips) {
    if (twips == null) {
        return null
    }
    try {
        return twips.toString().toDouble() / 20.0
    } catch (NumberFormatException ignored) {
        return null
    }
}

static Size twipsToSize(long twips) {
    return Size.ofPoints(twips == -1 ? 0 : twips / 20.0)
}

static Size emuToSize(long emu) {
    return Size.ofPoints(emu / EMU_PER_POINT)
}

static String sha256Hex(String value) {
    return MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8))
            .encodeHex()
            .toString()
}

static String calculateSHA(LinkedHashMap attributes) {
    return sha256Hex(attributes.values().collect { it.toString() }.join()).toUpperCase()
}

static boolean isHorizontallyMergedCell(XWPFTableCell cell) {
    return cell.CTTc.tcPr?.HMerge?.val?.toString() == "continue"
}

static String extractParagraphText(DocumentContent element) {
    if (!(element instanceof Paragraph)) {
        return null
    }
    return textValues(element)
            .findAll { it instanceof StringValue }
            .collect { (it as StringValue).value }
            .join()
}

private static List textValues(Paragraph paragraph) {
    return (paragraph.content ?: [])
            .findAll { it instanceof Paragraph.Text }
            .collectMany { (it as Paragraph.Text).content ?: [] }
}
