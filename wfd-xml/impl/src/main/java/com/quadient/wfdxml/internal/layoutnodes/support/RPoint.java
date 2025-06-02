package com.quadient.wfdxml.internal.layoutnodes.support;

import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class RPoint {
    private double x = 0;
    private double y = 0;

    public double getX() {
        return x;
    }

    public RPoint setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public RPoint setY(double y) {
        this.y = y;
        return this;
    }

    public void export(XmlExporter exporter, String elementName) {
        exporter.beginElement(elementName);
        exporter.addDoubleAttribute("X", x);
        exporter.addDoubleAttribute("Y", y);
        exporter.endElement();
    }
}
