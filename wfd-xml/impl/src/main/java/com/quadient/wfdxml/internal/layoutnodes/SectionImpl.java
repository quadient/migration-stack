package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Section;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

public class SectionImpl extends NodeImpl<Section> implements Section {

    private static final double DEFAULT_GUTTER_WIDTH = 0.0;
    private static final double DEFAULT_COLUMN_LINE_WIDTH = 0.0;

    private int numberOfColumns = 2;
    private double gutterWidth = DEFAULT_GUTTER_WIDTH;
    private BalancingType balancingType = BalancingType.FIRST_COLUMN;
    private ApplyTo applyTo = ApplyTo.WHOLE_TEMPLATE;

    @Override
    public SectionImpl setNumberOfColumns(int numberOfColumns) {
        if (numberOfColumns < 1) {
            throw new IllegalArgumentException("numberOfColumns must be >= 1, got: " + numberOfColumns);
        }
        this.numberOfColumns = numberOfColumns;
        return this;
    }

    @Override
    public SectionImpl setGutterWidth(double gutterWidth) {
        this.gutterWidth = gutterWidth;
        return this;
    }

    @Override
    public SectionImpl setBalancingType(BalancingType balancingType) {
        this.balancingType = balancingType;
        return this;
    }

    @Override
    public SectionImpl setApplyTo(ApplyTo applyTo) {
        this.applyTo = applyTo;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "Section";
    }

    @Override
    public void export(XmlExporter exporter) {
        String columnTypeValue = convertApplyToXml(applyTo);

        exporter.addElementWithStringData("ColumnType", columnTypeValue)
                .addElement("BorderStyleId")
                .addElementWithBoolData("EqualColumns", true);

        for (int i = 0; i < numberOfColumns; i++) {
            exporter.beginElement("Column")
                    .addElementWithDoubleData("ColumnWidth", 0.0)
                    .addElementWithDoubleData("GutterWidth", gutterWidth)
                    .addElement("BorderStyleId")
                    .endElement();
        }

        exporter.addElementWithDoubleData("ColumnLineWidth", DEFAULT_COLUMN_LINE_WIDTH)
                .addElement("ColumnFillStyleId")
                .addElementWithStringData("BalancingType", convertBalancingTypeToXml(balancingType))
                .addElement("FirstHeader")
                .addElement("Header")
                .addElement("Footer")
                .addElement("FirstFooter")
                .addElementWithStringData("ColumnType", columnTypeValue)
                .addElementWithStringData("SectionPageBreak", "None")
                .addElementWithBoolData("AutoFinish", true);
    }

    private static String convertBalancingTypeToXml(BalancingType balancingType) {
        return switch (balancingType) {
            case FIRST_COLUMN -> "FirstColumnBiggest";
            case BALANCED -> "Balanced";
            case UNBALANCED -> "Unbalanced";
        };
    }

    private static String convertApplyToXml(ApplyTo applyTo) {
        return switch (applyTo) {
            case WHOLE_TEMPLATE, THIS_BLOCK_ONLY -> "AutomaticColumns";
        };
    }
}
