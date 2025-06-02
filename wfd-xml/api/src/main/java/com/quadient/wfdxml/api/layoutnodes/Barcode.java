package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface Barcode<S extends LayoutObject<S>> extends LayoutObject<S> {

    S setBarcodeFill(FillStyle SFillStyle);

    S setBackgroundFill(FillStyle backgroundFillStyle);

    S setVariable(Variable variable);

    S setData(String data);

    S setUseEncoding(boolean useEncoding);

    S setEncoding(String encoding);

    S setSettingsFile(String settingsFileLocation);

    S setHorizontalAlign(HorizontalAlign align);

    S setVerticalAlign(VerticalAlign align);

    S setDataTextPosition(PositionType type);

    S setDataTextDeltaX(double dataTextDeltaX);

    S setDataTextDeltaY(double dataTextDeltaY);

    S setDataTextStyle(TextStyle dataTextStyle);

    S setShowDataTextProcessed(boolean dataTextProcessed);

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

    enum PositionType {
        NONE,
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }
}
