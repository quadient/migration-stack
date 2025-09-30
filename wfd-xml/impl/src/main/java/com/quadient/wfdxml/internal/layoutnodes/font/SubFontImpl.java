package com.quadient.wfdxml.internal.layoutnodes.font;

import com.quadient.wfdxml.api.layoutnodes.LocationType;
import com.quadient.wfdxml.api.layoutnodes.font.SubFont;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class SubFontImpl implements SubFont, XmlExportable {
    private int fontIndex = 0;
    private String fontLocation = "FONT_DIR,Arial.TTF";
    private String name = "Regular";
    private boolean bold = false;
    private boolean italic = false;

    public SubFontImpl() {
    }

    @Override
    public SubFont setFontIndex(int fontIndex) {
        this.fontIndex = fontIndex;
        return this;
    }

    @Override
    public SubFont setLocation(String location, LocationType locationType) {
        int lastSlash = location.lastIndexOf('/');
        int lastDot = location.lastIndexOf('.');
        if (lastDot <= lastSlash) {
            location = location + ".TTF";
        }

        this.fontLocation = locationType.getXmlValue() + "," + location;
        return this;
    }

    @Override
    public SubFont setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public SubFont setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public SubFontImpl setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getFontLocation() {
        return fontLocation;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isBold() {
        return bold;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addStringAttribute("Name", name).addBoolAttribute("Bold", bold).addBoolAttribute("Italic", italic).addElementWithInt64Data("FontIndex", fontIndex).addElementWithStringData("FontLocation", fontLocation);
    }
}
