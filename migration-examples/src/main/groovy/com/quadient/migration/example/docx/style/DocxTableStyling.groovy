package com.quadient.migration.example.docx.style

import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.BorderOptionsBuilder
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders

import static com.quadient.migration.example.docx.util.DocxUtils.twipsToPoints

static void applyColumnWidths(TableBuilder tableBuilder, XWPFTable table) {
    def gridCols = table.getCTTbl()?.getTblGrid()?.getGridColList()
    if (!gridCols) {
        return
    }
    List<Long> widthsTwips = gridCols.collect { it.getW() }
    long totalTwips = widthsTwips.sum() as long
    if (totalTwips <= 0) {
        return
    }
    List<Double> percentages = widthsTwips.collect { it / (double) totalTwips }
    // Correct any floating point rounding drift so the fractions still sum to exactly 1.0
    percentages[-1] += 1.0 - percentages.sum()

    List<TableBuilder.ColumnWidth> columnWidths = percentages.collect { new TableBuilder.ColumnWidth(Size.ofMeters(0.001), it) }
    tableBuilder.columnWidths(columnWidths)
}

static void applyRowHeight(TableBuilder.Cell cellBuilder, XWPFTableRow row) {
    def trHeight = row.ctRow?.trPr?.trHeightList?.find { it.isSetVal() }
    if (trHeight == null || (trHeight.val as long) <= 0) {
        return
    }
    Size height = Size.ofPoints(twipsToPoints(trHeight.val))
    switch (trHeight.isSetHRule() ? trHeight.HRule.toString() : "auto") {
        case "exact":
            cellBuilder.heightFixed(height)
            break
        case "atLeast":
            cellBuilder.heightCustom(height, Size.ofPoints(height.toPoints() * 10))
            break
        default:
            break // auto (or unset): let the cell grow with its content
    }
}

static void applyCellStyling(TableBuilder.Cell cellBuilder, XWPFTableCell cell, XWPFTable table,
                             int rowIndex = 0, int columnIndex = 0, int rowCount = 1, int columnCount = 1) {
    def tblPr = table.getCTTbl()?.getTblPr()
    def tcPr = cell.getCTTc()?.getTcPr()

    def tblBorders = tblPr?.getTblBorders()
    def styleBorders = tableStyleBorders(table)
    def tcBorders = tcPr?.getTcBorders()
    Color fill = resolveShdFill(tcPr?.getShd()) ?: resolveShdFill(tblPr?.getShd())
    def tblCellMar = tblPr?.getTblCellMar()
    def tcMar = tcPr?.getTcMar()
    CellAlignment alignment = resolveVerticalAlignment(tcPr)

    if (fill == null && tblBorders == null && styleBorders == null && tcBorders == null && tblCellMar == null && tcMar == null && alignment == null) {
        return
    }
    if (alignment != null) {
        cellBuilder.alignment(alignment)
    }
    cellBuilder.border { BorderOptionsBuilder builder ->
        applyBorderLine(builder.&leftLine, firstUsable(tcBorders?.getLeft(), columnIndex == 0 ? tblBorders?.getLeft() : null,
                columnIndex == 0 ? styleBorders?.getLeft() : null, columnIndex > 0 ? (tblBorders?.getInsideV() ?: styleBorders?.getInsideV()) : null))
        applyBorderLine(builder.&rightLine, firstUsable(tcBorders?.getRight(), columnIndex == columnCount - 1 ? tblBorders?.getRight() : null,
                columnIndex == columnCount - 1 ? styleBorders?.getRight() : null))
        applyBorderLine(builder.&topLine, firstUsable(tcBorders?.getTop(), rowIndex == 0 ? tblBorders?.getTop() : null,
                rowIndex == 0 ? styleBorders?.getTop() : null, rowIndex > 0 ? (tblBorders?.getInsideH() ?: styleBorders?.getInsideH()) : null))
        applyBorderLine(builder.&bottomLine, firstUsable(tcBorders?.getBottom(), rowIndex == rowCount - 1 ? tblBorders?.getBottom() : null,
                rowIndex == rowCount - 1 ? styleBorders?.getBottom() : null))
        if (fill != null) {
            builder.fill(fill)
        }
        applyPaddingSide(builder.&paddingLeft, tcMar?.getLeft(), tblCellMar?.getLeft())
        applyPaddingSide(builder.&paddingRight, tcMar?.getRight(), tblCellMar?.getRight())
        applyPaddingSide(builder.&paddingTop, tcMar?.getTop(), tblCellMar?.getTop())
        applyPaddingSide(builder.&paddingBottom, tcMar?.getBottom(), tblCellMar?.getBottom())
    }
}

private static CellAlignment resolveVerticalAlignment(def tcPr) {
    def vAlign = tcPr?.getVAlign()?.getVal()
    if (vAlign == null) {
        return null
    }
    switch (vAlign.toString()) {
        case "center": return CellAlignment.Center
        case "bottom": return CellAlignment.Bottom
        case "top": return CellAlignment.Top
        case "both": return CellAlignment.Top // "both" (justify) has no direct equivalent; top is the closest fallback
        default: return null
    }
}

private static Color resolveShdFill(CTShd shd) {
    if (shd == null) {
        return null
    }
    return StyleChainResolver.toColor(shd.getFill()) ?: StyleChainResolver.toColor(shd.getColor())
}

private static CTBorder firstUsable(CTBorder... borders) {
    return borders.find { isUsableBorder(it) }
}

private static CTTblBorders tableStyleBorders(XWPFTable table) {
    String styleId = table.CTTbl?.tblPr?.tblStyle?.val
    def styles = table.rows.findResult { it.tableCells.findResult { cell -> cell.paragraphs[0]?.document?.styles } }
    Set<String> visited = []
    while (styleId && visited.add(styleId)) {
        def style = styles?.getStyle(styleId)?.CTStyle
        def borders = style?.tblPr?.tblBorders
        if (borders != null) return borders
        styleId = style?.basedOn?.val
    }
    return null
}

private static boolean isUsableBorder(CTBorder border) {
    if (border == null) {
        return false
    }
    String style = border.getVal()?.toString()
    return style && style != "none" && style != "nil"
}

private static void applyBorderLine(Closure<BorderOptionsBuilder> lineSetter, CTBorder border) {
    if (!isUsableBorder(border)) {
        return
    }
    Color color = StyleChainResolver.toColor(border.getColor())
    // w:sz is in eighths of a point
    Size width = border.getSz() != null ? Size.ofPoints(border.getSz().doubleValue() / 8.0) : null
    lineSetter(color, width)
}

private static void applyPaddingSide(Closure<BorderOptionsBuilder> paddingSetter, def cellSideMargin, def tableSideMargin) {
    Size padding = resolveMarginSize(cellSideMargin) ?: resolveMarginSize(tableSideMargin)
    if (padding != null) {
        paddingSetter(padding)
    }
}

private static Size resolveMarginSize(def tblWidth) {
    if (tblWidth == null || !tblWidth.isSetW()) {
        return null
    }
    String type = tblWidth.getType()?.toString()
    if (type == "nil") {
        return Size.ofPoints(0)
    }
    if (type == "pct") {
        return null // percentage-based margins aren't supported here
    }
    Double points = twipsToPoints(tblWidth.w)
    return points != null ? Size.ofPoints(points) : null
}
