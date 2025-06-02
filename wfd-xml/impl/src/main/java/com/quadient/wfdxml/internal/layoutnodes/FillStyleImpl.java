package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Color;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class FillStyleImpl extends NodeImpl<FillStyle> implements FillStyle {
    private ColorImpl color;

    @Override
    public FillStyle setColor(Color color) {
        this.color = (ColorImpl) color;
        return this;
    }

    public ColorImpl getColor() {
        return color;
    }


    @Override
    public String getXmlElementName() {
        return "FillStyle";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithIface("ColorId", color);
    }
}
