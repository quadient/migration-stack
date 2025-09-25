package com.quadient.wfdxml.api.layoutnodes.font;

import com.quadient.wfdxml.api.layoutnodes.LocationType;

public interface SubFont {
    SubFont setFontIndex(int fontIndex);

    SubFont setLocation(String location, LocationType locationType);

    SubFont setItalic(boolean italic);

    SubFont setBold(boolean bold);

    boolean isItalic();

    boolean isBold();
}
