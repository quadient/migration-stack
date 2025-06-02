package com.quadient.wfdxml.internal.layoutnodes.support;

import com.quadient.wfdxml.api.layoutnodes.TabulatorType;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.DECIMAL;

public class Tabulator implements XmlExportable {
    private final double pos;
    private final TabulatorType type;
    private String separator;

    public Tabulator(double pos, TabulatorType type) {
        this.pos = pos;
        this.type = type;
    }

    public Tabulator(double pos, String point) {
        this.pos = pos;
        this.type = DECIMAL;
        this.separator = point;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("Tabulator")
                .addElementWithStringData("Type", convertTabulatorTypeToXmlName())
                .addElementWithStringData("Separator", separator)
                .addElementWithDoubleData("Pos", pos)
                .endElement();
    }

    public String convertTabulatorTypeToXmlName() {
        switch (type) {
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            case CENTER:
                return "Center";
            case DECIMAL:
                return "Decimal";
            case WORD_DECIMAL:
                return "WordDecimal";
            default:
                throw new IllegalStateException(type.toString());
        }
    }
}