package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Font;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.layoutnodes.support.SubFont;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.nio.file.Paths;

public class FontImpl extends NodeImpl<Font> implements Font {
    private static final String DEFAULT_FONT = "Arial";
    private SubFont subFont = new SubFont();

    public FontImpl() {
        setName(DEFAULT_FONT);
    }

    @Override
    public FontImpl setFont(String font) {
        subFont = new SubFont().setSubFont(font);
        setName(font);
        return this;
    }

    @Override
    public FontImpl setFontFromDiskLocation(String fontPath) {
        subFont = new SubFont().setSubFontDiskLocation(fontPath);
        String fontName = Paths.get(fontPath).getFileName().toString();
        if (fontName != null) {
            setName(fontName);
        }
        return this;
    }

    public FontImpl setItalic(boolean italic) {
        subFont.setItalic(italic);
        return this;
    }

    public FontImpl setBold(boolean bold) {
        subFont.setBold(bold);
        return this;
    }

    public SubFont getSubFont() {
        return subFont;
    }

    @Override
    public String getXmlElementName() {
        return "Font";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("SubFont")
                .addStringAttribute("Name", "Regular")
                .addBoolAttribute("Bold", subFont.isBold())
                .addBoolAttribute("Italic", subFont.isItalic())
                .addElementWithInt64Data("FontIndex", subFont.getFontIndex())
                .addElementWithStringData("FontLocation", subFont.getFontLocation());
        exporter.endElement();
    }
}
