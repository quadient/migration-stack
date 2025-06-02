package com.quadient.wfdxml.api.layoutnodes.tables;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface Table extends Node<Table> {

    Table setRowSet(RowSet rowSet);

    Table addColumn();

    Table addColumn(double minWidth, double sizeRatio);

    Table addColumn(double minWidth, double sizeRatio, Variable enableBy, boolean header);

    Table setBorderStyle(BorderStyle borderStyle);

    Table setBorderType(BordersType borderType);

    Table setAlignment(TableAlignment alignment);

    Table setPercentWidth(double percentWidth);

    Table setPercentWidthDisable();

    Table setMinWidth(double minWidth);

    Table setMinWidthDisable();

    Table setMaxWidth(double maxWidth);

    Table setMaxWidthDisable();

    Table setIncludeLineGap(boolean includeLineGab);

    Table setApplyHtmlFormatting(boolean applyHtmlFormatting);

    Table setResponsiveHtml(boolean responsiveHtml);

    Table setHorizontalSpacing(double horizontalSpacing);

    Table setVerticalSpacing(double verticalSpacing);

    Table setSpaceLeft(double spaceLeft);

    Table setSpaceTop(double spaceTop);

    Table setSpaceRight(double spaceRight);

    Table setSpaceBottom(double spaceBottom);

    Table setDisplayAsImage(boolean displayAsImage);

    Table setEditability(EditabilityType editability);


    enum EditabilityType {
        LABEL_AND_LOCK,
        LOCK
    }

    enum TableAlignment {
        LEFT,
        RIGHT,
        CENTER,
        INHERIT
    }

    enum BordersType {
        SIMPLE,
        MERGE_BORDERS
    }
}