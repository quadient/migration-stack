package com.quadient.wfdxml.api.layoutnodes.tables;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.LineStyle;

public interface BorderStyle extends Node<BorderStyle> {

    SelectedLinesAndCorners select(LinesAndCorners... linesAndCorners);

    BorderStyle setOffsets(double top, double right, double bottom, double left);

    BorderStyle setOffsetTop(double top);

    BorderStyle setOffsetRight(double right);

    BorderStyle setOffsetBottom(double bottom);

    BorderStyle setOffsetLeft(double left);

    BorderStyle setMargins(double top, double right, double bottom, double left);

    BorderStyle setMarginTop(double top);

    BorderStyle setMarginRight(double right);

    BorderStyle setMarginBottom(double bottom);

    BorderStyle setMarginLeft(double left);

    BorderStyle setJoin(JoinType join);

    BorderStyle setMiter(double miter);

    BorderStyle setFill(FillStyle fill);

    BorderStyle setShadowFillStyle(FillStyle shadowFillStyle);

    BorderStyle setShadowOffsetX(double shadowOffsetX);

    BorderStyle setShadowOffsetY(double shadowOffsetY);

    interface SelectedLinesAndCorners {
        BorderStyle backToBorderStyle();

        SelectedLinesAndCorners setLineFillStyle(FillStyle lineFillStyle);

        SelectedLinesAndCorners setLineWidth(double lineWidth);

        SelectedLinesAndCorners setCap(CapType cap);

        SelectedLinesAndCorners setLineStyle(LineStyle lineStyle);

        SelectedLinesAndCorners setCorner(CornerType corner);

        SelectedLinesAndCorners setRadiusX(double radiusX);

        SelectedLinesAndCorners setRadiusY(double radiusY);
    }

    enum LinesAndCorners {
        ALL_BORDERS,
        ALL_LINES,
        ALL_CORNERS,
        LEFT_LINE,
        TOP_LINE,
        RIGHT_LINE,
        BOTTOM_LINE,
        MAIN_DIAGONAL_LINE,
        SECONDARY_DIAGONAL_LINE,
        UPPER_LEFT_CORNER,
        UPPER_RIGHT_CORNER,
        LOWER_RIGHT_CORNER,
        LOWER_LEFT_CORNER,
    }

    enum JoinType {
        MITER,
        ROUND,
        BEVEL,
    }

    enum CornerType {
        STANDARD,
        ROUND,
        ROUND_OUT,
        CUT_OUT,
        LINE_CORNER,
    }

    enum CapType {
        BUTT,
        ROUND,
        SQUARE,
    }
}