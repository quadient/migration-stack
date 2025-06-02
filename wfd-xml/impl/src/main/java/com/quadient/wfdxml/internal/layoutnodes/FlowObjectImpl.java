package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Element;
import com.quadient.wfdxml.api.layoutnodes.PathObject;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.module.layout.LayoutImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class FlowObjectImpl extends Tree<Element> implements Element {
    private final LayoutImpl layout;
    private Formatting verticalObjectPosition = Formatting.BASELINE;
    private boolean runaroundByChildren = false;
    private boolean dynamicSize = false;
    private double sizeX = 0.0d;
    private double sizeY = 0.0d;
    private double offset = 0.0d;

    public FlowObjectImpl(LayoutImpl layout) {
        this.layout = layout;
    }

    public static String convertFormattingKindToXmlName(Formatting kind) {
        switch (kind) {
            case TOP:
                return "Top";
            case BOTTOM:
                return "Bottom";
            case CENTER:
                return "Center";
            case BASELINE:
                return "BaseLine";
            default:
                throw new IllegalStateException(kind.toString());
        }
    }

    @Override
    public Element setWidth(double width) {
        this.sizeX = width;
        return this;
    }

    @Override
    public Element setHeight(double height) {
        this.sizeY = height;
        return this;
    }

    @Override
    public Element setFormatting(Formatting kind) {
        this.verticalObjectPosition = kind;
        return this;
    }

    @Override
    public Element setOffset(double offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public Element setRunaroundChildrenOnly(boolean runaround) {
        this.runaroundByChildren = runaround;
        return this;
    }

    @Override
    public Element setDynamicSizeByRunaround(boolean dynamicSizeByRunaround) {
        this.dynamicSize = dynamicSizeByRunaround;
        return this;
    }

    @Override
    public PathObjectImpl addPathObject() {
        PathObjectImpl pathObject = new PathObjectImpl();
        pathObject.setLineFillStyle(layout == null ? null : layout.getDefFillStyle());
        children.add(pathObject);
        return pathObject;
    }

    @Override
    public PathObject addLine(double x1, double y1, double x2, double y2) {
        PathObject pathObject = addPathObject();
        pathObject
                .setPositionAndSize(0, 0, (x2 - x1), (y2 - y1))
                .addLineTo((x2 - x1), (y2 - y1));
        return pathObject;
    }

    @Override
    public BarcodeFactoryImpl getBarcodeFactory() {
        return new BarcodeFactoryImpl(children, layout.getDefTextStyle());
    }

    @Override
    public String getXmlElementName() {
        return "FlowObject";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("Size")
                .addDoubleAttribute("X", sizeX)
                .addDoubleAttribute("Y", sizeY)
                .endElement()
                .addElementWithDoubleData("Offset", offset)
                .addElementWithStringData("VerticalObjectPosition", convertFormattingKindToXmlName(verticalObjectPosition))
                .addElementWithBoolData("RunaroundByChildren", runaroundByChildren)
                .addElementWithBoolData("DynamicSize", dynamicSize);
    }
}