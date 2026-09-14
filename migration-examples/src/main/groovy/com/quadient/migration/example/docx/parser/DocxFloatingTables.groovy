package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPPr

import static com.quadient.migration.example.docx.util.DocxUtils.twipsToPoints

class DocxFloatingTables {
    private static final double MIN_AREA_HEIGHT_PT = 10.0d

    static boolean isFloating(XWPFTable table) {
        return table.CTTbl?.tblPr?.tblpPr != null
    }

    static Area buildFloatingTableArea(XWPFTable table, List<DocumentContent> contentItems, Position pagePosition) {
        if (contentItems.isEmpty()) {
            return null
        }
        Position position = resolveTablePosition(table, table.CTTbl.tblPr.tblpPr, pagePosition)
        return new AreaBuilder().content(contentItems).position(position).build()
    }

    private static Position resolveTablePosition(XWPFTable table, CTTblPPr tblpPr, Position pagePosition) {
        double marginLeft = pagePosition.x.toPoints()
        double marginTop = pagePosition.y.toPoints()

        double x = marginLeft
        if (tblpPr.isSetTblpX()) {
            x = twipsToPoints(tblpPr.tblpX) + (tblpPr.horzAnchor?.toString() == "page" ? 0.0d : marginLeft)
        }
        double y = marginTop
        if (tblpPr.isSetTblpY()) {
            y = twipsToPoints(tblpPr.tblpY) + (tblpPr.vertAnchor?.toString() == "page" ? 0.0d : marginTop)
        }

        double contentBottom = marginTop + pagePosition.height.toPoints()
        double height = Math.max(contentBottom - y, MIN_AREA_HEIGHT_PT)
        return new Position(Size.ofPoints(x), Size.ofPoints(y), resolveTableWidth(table, pagePosition.width), Size.ofPoints(height))
    }

    private static Size resolveTableWidth(XWPFTable table, Size fallbackWidth) {
        def gridCols = table.CTTbl.tblGrid?.gridColList
        if (!gridCols) {
            return fallbackWidth
        }
        double totalPoints = gridCols.sum { twipsToPoints(it.w) ?: 0.0d } as double
        return totalPoints > 0 ? Size.ofPoints(totalPoints) : fallbackWidth
    }
}
