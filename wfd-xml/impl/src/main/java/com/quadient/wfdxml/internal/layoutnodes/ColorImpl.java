package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Color;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.layoutnodes.support.ColorRgb;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class ColorImpl extends NodeImpl<Color> implements Color {
    private ColorRgb colorRgb = new ColorRgb();

    @Override
    public ColorImpl setRGB(double red, double green, double blue) {
        colorRgb = new ColorRgb().setRgb(red, green, blue);
        return this;
    }

    @Override
    public ColorImpl setRGB(int red, int green, int blue) {
        return setRGB(red / 255.0, green / 255.0, blue / 255.0);
    }

    @Override
    public ColorImpl setRGB(java.awt.Color awtColor) {
        return setRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
    }

    @Override
    public String getXmlElementName() {
        return "Color";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithStringData("RGB", rgbXmlFormat());
    }

    private String rgbXmlFormat() {
        return colorRgb.getRed() + "," + colorRgb.getGreen() + "," + colorRgb.getBlue();
    }

    @Override
    public String toString() {
        return rgbXmlFormat();
    }

    public ColorRgb getColorRgb() {
        return colorRgb;
    }
}
