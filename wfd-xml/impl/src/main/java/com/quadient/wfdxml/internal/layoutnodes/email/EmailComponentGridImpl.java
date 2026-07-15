package com.quadient.wfdxml.internal.layoutnodes.email;

import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentContent;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.utils.FormatUtils.formatPx;

import java.util.ArrayList;
import java.util.List;

public class EmailComponentGridImpl extends NodeImpl<EmailComponentGrid> implements EmailComponentGrid {
    private final List<Column> columns = new ArrayList<>();
    private boolean fullWidthBackground = false;
    private ColumnDistribution distribution = ColumnDistribution.EVEN_WIDTH;
    private VerticalAlignment verticalAlignment = VerticalAlignment.TOP;
    private OnMobile onMobile = OnMobile.FROM_LEFT;
    private double paddingLeft = 0;
    private double paddingTop = 0;
    private double paddingRight = 0;
    private double paddingBottom = 0;
    private FillStyle fillStyle;

    @Override
    public Column addColumn() {
        var result = new Column();
        columns.add(result);
        return result;
    }

    @Override
    public EmailComponentGrid setFullWidthBackground(boolean fullWidthBackground) {
        this.fullWidthBackground = fullWidthBackground;
        return this;
    }

    @Override
    public EmailComponentGrid setDistribution(ColumnDistribution distribution) {
        this.distribution = distribution;
        return this;
    }

    @Override
    public EmailComponentGrid setVerticalAlignment(VerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        return this;
    }

    @Override
    public EmailComponentGrid setOnMobile(OnMobile onMobile) {
        this.onMobile = onMobile;
        return this;
    }

    @Override
    public EmailComponentGrid setPaddingLeft(double paddingLeft) {
        this.paddingLeft = paddingLeft;
        return this;
    }

    @Override
    public EmailComponentGrid setPaddingTop(double paddingTop) {
        this.paddingTop = paddingTop;
        return this;
    }

    @Override
    public EmailComponentGrid setPaddingRight(double paddingRight) {
        this.paddingRight = paddingRight;
        return this;
    }

    @Override
    public EmailComponentGrid setPaddingBottom(double paddingBottom) {
        this.paddingBottom = paddingBottom;
        return this;
    }

    @Override
    public EmailComponentGrid setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "ECGrid";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("Columns");
        for (Column column: columns) {
            exporter.beginElement("Column");
            for (EmailComponentContent content : column.getContent()) {
                exporter.addElementWithIface("Component", content);
            }
            exporter.endElement();
        }
        exporter.endElement();

        exporter.addElementWithIntData("ColumnsCount", columns.size());
        exporter.addElementWithBoolData("FullWidthBackground", fullWidthBackground);
        exporter.addElementWithStringData("Distribution", distributionToXml(distribution));
        exporter.addElementWithStringData("VerticalAlignment", verticalAlignmentToXml(verticalAlignment));
        exporter.addElementWithStringData("OnMobile", onMobileToXml(onMobile));

        exporter.beginElement("Padding");
        exporter.addElementWithStringData("Left", formatPx(paddingLeft));
        exporter.addElementWithStringData("Top", formatPx(paddingTop));
        exporter.addElementWithStringData("Right", formatPx(paddingRight));
        exporter.addElementWithStringData("Bottom", formatPx(paddingBottom));
        exporter.endElement();

        if (fillStyle != null) {
            exporter.addElementWithIface("FillStyleId", fillStyle);
        } else {
            exporter.addElement("FillStyleId");
        }
    }

    private String distributionToXml(ColumnDistribution distribution) {
        switch (distribution) {
            case EVEN_WIDTH:
                return "None";
            case TWO_COLUMNS_25_75:
                return "25-75";
            case TWO_COLUMNS_33_66:
                return "33-66";
            case TWO_COLUMNS_66_33:
                return "66-33";
            case TWO_COLUMNS_75_25:
                return "75-25";
            case THREE_COLUMNS_25_25_50:
                return "25-25-50";
            case THREE_COLUMNS_25_50_25:
                return "25-50-25";
            case THREE_COLUMNS_50_25_25:
                return "50-25-25";
            default:
                throw new IllegalStateException(distribution.toString());
        }
    }

    private String verticalAlignmentToXml(VerticalAlignment verticalAlignment) {
        switch (verticalAlignment) {
            case TOP:
                return "Top";
            case BOTTOM:
                return "Bottom";
            case CENTER:
                return "Center";
            default:
                throw new IllegalStateException(verticalAlignment.toString());
        }
    }

    private String onMobileToXml(OnMobile onMobile) {
        switch (onMobile) {
            case FROM_LEFT:
                return "FromLeft";
            case FROM_RIGHT:
                return "FromRight";
            case NO_STACKING:
                return "NoStacking";
            default:
                throw new IllegalStateException(onMobile.toString());
        }
    }
}
