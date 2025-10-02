package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Font;
import com.quadient.wfdxml.api.layoutnodes.font.SubFont;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.layoutnodes.font.SubFontImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

public class FontImpl extends NodeImpl<Font> implements Font {
    private String fontName;
    private final List<SubFont> subFonts = new ArrayList<>();

    public FontImpl() {
        setName("Arial");
    }

    @Override
    public Font setFontName(String fontName) {
        this.fontName = fontName;
        return this;
    }

    @Override
    public SubFont addSubfont() {
        var subfont = new SubFontImpl();
        subFonts.add(subfont);
        return subfont;
    }

    @Override
    public List<SubFont> getSubFonts() {
        return subFonts;
    }

    @Override
    public String getXmlElementName() {
        return "Font";
    }

    @Override
    public void export(XmlExporter exporter) {
        if (fontName != null && !fontName.isBlank()) {
            exporter.addElementWithStringData("FontName", fontName);
        }

        subFonts.forEach(subFont -> {
            exporter.beginElement("SubFont");
            ((SubFontImpl) subFont).export(exporter);
            exporter.endElement();
        });
    }
}
