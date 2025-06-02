package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.LineStyle;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class LineStyleImpl extends NodeImpl<LineStyle> implements LineStyle {
    private double beginOffset = 0.005;
    private Vector<Double> doubleVector = new Vector<>();

    private Endings lineEndingStart = Endings.NONE;
    private Endings lineEndingEnd = Endings.NONE;
    private double lineEndingRatio = 3.0;
    private List<Stripe> stripes = new ArrayList<>();

    public static int convertEndingsToInt(Endings endings) {
        switch (endings) {
            case NONE:
                return -1;
            case ORTHO_LINE:
                return 0;
            case QUARK_ARROW:
                return 1;
            case QUARK_FEATHER:
                return 2;
            case SOLID_SQUARE:
                return 3;
            case SQUARE:
                return 4;
            case SOLID_CIRCLE:
                return 5;
            case CIRCLE:
                return 6;
            case CURVED_ARROW:
                return 7;
            case BARBED_ARROW:
                return 8;
            case TRIANGLE_WIDE:
                return 9;
            case TRIANGLE:
                return 10;
            case SIMPLE_WIDE:
                return 11;
            case SIMPLE:
                return 12;
            default:
                throw new IllegalStateException(endings.toString());
        }
    }

    public double getBeginOffset() {
        return beginOffset;
    }

    @Override
    public LineStyleImpl setBeginOffset(double beginOffset) {
        this.beginOffset = beginOffset;
        return this;
    }

    public Vector<Double> getDoubleVector() {
        return doubleVector;
    }

    public LineStyleImpl setDoubleVector(Vector<Double> doubleVector) {
        this.doubleVector = doubleVector;
        return this;
    }

    public Endings getLineEndingStart() {
        return lineEndingStart;
    }

    public LineStyleImpl setLineEndingStart(Endings lineEndingStart) {
        this.lineEndingStart = lineEndingStart;
        return this;
    }

    public Endings getLineEndingEnd() {
        return lineEndingEnd;
    }

    public LineStyleImpl setLineEndingEnd(Endings lineEndingEnd) {
        this.lineEndingEnd = lineEndingEnd;
        return this;
    }

    public double getLineEndingRatio() {
        return lineEndingRatio;
    }

    public LineStyleImpl setLineEndingRatio(double lineEndingRatio) {
        this.lineEndingRatio = lineEndingRatio;
        return this;
    }

    public List<Stripe> getStripes() {
        return stripes;
    }

    public LineStyleImpl setStripes(List<Stripe> stripes) {
        this.stripes = stripes;
        return this;
    }

    @Override
    public LineStyleImpl addLineGapValues(double lineGapValues) {
        doubleVector.add(lineGapValues);
        return this;
    }

    @Override
    public LineStyleImpl addStripe(double position, double width) {
        stripes.add(new Stripe(position, width));
        return this;
    }

    @Override
    public LineStyleImpl setStartEnding(Endings start) {
        setLineEndingStart(start);
        return this;
    }

    @Override
    public LineStyleImpl setEndEnding(Endings end) {
        setLineEndingEnd(end);
        return this;
    }

    @Override
    public LineStyleImpl setSizeEnding(double size) {
        setLineEndingRatio(size);
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "LineStyle";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithDoubleData("BeginOffset", beginOffset)
                .addElementWithStringData("DoubleVector", doubleVectorExport(doubleVector))
                .beginElement("Stripes");

        if (stripes.isEmpty()) {
            stripes.add(new Stripe(0, 1));
        }

        for (Stripe stripe : stripes) {
            exporter.beginElement("Stripe")
                    .addDoubleAttribute("Pos", stripe.getPos())
                    .addDoubleAttribute("Width", stripe.getWidth())
                    .endElement();
        }

        exporter.endElement()
                .addElementWithIntData("LineEndingStart", convertEndingsToInt(lineEndingStart))
                .addElementWithIntData("LineEndingEnd", convertEndingsToInt(lineEndingEnd))
                .addElementWithDoubleData("LineEndingRatio", lineEndingRatio);
    }

    public String doubleVectorExport(Vector<Double> vector) {
        if (vector.isEmpty()) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < vector.size(); i++) {
            if (i != 0) {
                str.append(",");
            }
            str.append(vector.get(i));
        }
        return str.toString();
    }

    public class Stripe {
        private double pos;
        private double width;

        public Stripe(double pos, double width) {
            this.pos = pos;
            this.width = width;
        }

        public double getPos() {
            return pos;
        }

        public Stripe setPos(double pos) {
            this.pos = pos;
            return this;
        }

        public double getWidth() {
            return width;
        }

        public Stripe setWidth(double width) {
            this.width = width;
            return this;
        }
    }
}