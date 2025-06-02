package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface ImageArea extends LayoutObject<ImageArea> {

    ImageArea setImage(Image image);

    ImageArea setPreserveAspectRatio(boolean preserve);

    ImageArea setStretch(boolean stretch);

    ImageArea setShrink(boolean shrink);

    ImageArea setHorizontalAlign(HorizontalAlign align);

    ImageArea setVerticalAlign(VerticalAlign align);

    ImageArea setUrlLink(Variable variable);

    ImageArea setClipImage(boolean clip);

    ImageArea setOffsetX(double x);

    ImageArea setOffsetY(double y);

    enum HorizontalAlign {
        LEFT,
        CENTER,
        RIGHT
    }

    enum VerticalAlign {
        TOP,
        CENTER,
        BOTTOM
    }
}