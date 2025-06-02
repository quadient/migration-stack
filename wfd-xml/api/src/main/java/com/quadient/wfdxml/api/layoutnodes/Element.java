package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface Element extends Node<Element> {

    Element setWidth(double width);

    Element setHeight(double height);

    Element setFormatting(Formatting kind);

    Element setOffset(double offset);

    Element setRunaroundChildrenOnly(boolean runaround);

    Element setDynamicSizeByRunaround(boolean dynamicSizeByRunaround);

    PathObject addLine(double x1, double y1, double x2, double y2);

    PathObject addPathObject();

    BarcodeFactory getBarcodeFactory();

    enum Formatting {
        BASELINE,
        TOP,
        CENTER,
        BOTTOM
    }
}
