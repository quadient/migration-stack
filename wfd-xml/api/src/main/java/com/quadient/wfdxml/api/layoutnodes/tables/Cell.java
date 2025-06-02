package com.quadient.wfdxml.api.layoutnodes.tables;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.Flow;

public interface Cell extends Node<Cell> {

    Cell setFlow(Flow flow);

    Cell setBorderStyle(BorderStyle borderStyle);

    Cell setAlignment(CellVerticalAlignment alignment);

    Cell setFitting(FittingType fitting);

    Cell setType(CellType type);

    /**
     * @param fixedHeight in meters
     */
    Cell setFixedHeight(double fixedHeight);

    Cell setHtmlWidth(HTMLWidthType htmlWidth);

    /**
     * @param minHeight in meters
     */
    Cell setMinHeight(double minHeight);

    /**
     * @param maxHeight in meters
     */
    Cell setMaxHeight(double maxHeight);

    /**
     * @param fixedWidth in meters
     */
    Cell setFixedWidth(double fixedWidth);

    Cell setSpanLeft(boolean spanLeft);

    Cell setSpanUp(boolean spanUp);

    Cell setFlowToNextPage(boolean flowToNextPage);

    Cell setAlwaysProcess(boolean alwaysProcess);

    Cell setFillRelativeToCell(boolean fillRelativeToCell);

    Cell setWidthInPercent(double widthInPercent);

    enum CellVerticalAlignment {
        TOP,
        BOTTOM,
        CENTER,
    }

    enum HTMLWidthType {
        AUTO,
        LENGTH,
        PERCENT,
    }

    enum FittingType {
        NONE,
        HORIZONTAL,
        VERTICAL,
        BOTH,
    }

    enum CellType {
        CUSTOM,
        FIXED_HEIGHT,
    }
}