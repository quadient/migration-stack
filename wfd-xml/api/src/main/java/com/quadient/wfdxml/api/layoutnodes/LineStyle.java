package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface LineStyle extends Node<LineStyle> {

    LineStyle setBeginOffset(double beginOffset);

    LineStyle addLineGapValues(double lineGapValues);

    /**
     * set pos in range &lt;-1;1&gt;, width &lt;0;1&gt;
     */
    LineStyle addStripe(double position, double width);

    LineStyle setStartEnding(Endings start);

    LineStyle setEndEnding(Endings end);

    LineStyle setSizeEnding(double size);

    enum Endings {
        NONE,
        ORTHO_LINE,
        QUARK_ARROW,
        QUARK_FEATHER,
        SOLID_SQUARE,
        SQUARE,
        SOLID_CIRCLE,
        CIRCLE,
        CURVED_ARROW,
        BARBED_ARROW,
        TRIANGLE_WIDE,
        TRIANGLE,
        SIMPLE_WIDE,
        SIMPLE,
    }
}
