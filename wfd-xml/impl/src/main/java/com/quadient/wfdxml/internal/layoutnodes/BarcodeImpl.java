package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Barcode;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

abstract class BarcodeImpl<S extends Barcode<S>> extends LayoutObjectImpl<S> implements Barcode<S> {
    private Variable variable;
    private FillStyle barcodeFillStyle;
    private FillStyle backgroundFillStyle;
    private String data;
    private String settingsLocation;

    private String encoding = "UTF-8";
    private boolean useEncoding = false;
    private HorizontalAlign horizontalAlign = HorizontalAlign.LEFT;
    private VerticalAlign verticalAlign = VerticalAlign.TOP;
    private PositionType positionType = PositionType.NONE;
    private double dataTextDeltaX;
    private double dataTextDeltaY;
    private TextStyle textStyle;
    private boolean dataTextProcessed;

    public static String convertHorizontalAlignToXmlName(HorizontalAlign align) {
        switch (align) {
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            case CENTER:
                return "Center";
            default:
                throw new IllegalStateException(align.toString());
        }
    }

    public static String convertVerticalAlignToXmlName(VerticalAlign align) {
        switch (align) {
            case TOP:
                return "Top";
            case CENTER:
                return "Center";
            case BOTTOM:
                return "Bottom";
            default:
                throw new IllegalStateException(align.toString());
        }
    }

    public static String convertPositionTypeToXmlName(PositionType type) {
        switch (type) {
            case NONE:
                return "None";
            case TOP_LEFT:
                return "TopLeft";
            case TOP_CENTER:
                return "TopCenter";
            case TOP_RIGHT:
                return "TopRight";
            case BOTTOM_LEFT:
                return "BottomLeft";
            case BOTTOM_CENTER:
                return "BottomCenter";
            case BOTTOM_RIGHT:
                return "BottomRight";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setBarcodeFill(FillStyle barcodeFillStyle) {
        this.barcodeFillStyle = barcodeFillStyle;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setBackgroundFill(FillStyle backgroundFillStyle) {
        this.backgroundFillStyle = backgroundFillStyle;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setVariable(Variable variable) {
        this.variable = variable;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setData(String data) {
        this.data = data;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setUseEncoding(boolean useEncoding) {
        this.useEncoding = useEncoding;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setEncoding(String encoding) {
        this.encoding = encoding;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setSettingsFile(String settingsLocation) {
        this.settingsLocation = "DiskLocation," + settingsLocation;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setHorizontalAlign(HorizontalAlign align) {
        this.horizontalAlign = align;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setVerticalAlign(VerticalAlign align) {
        this.verticalAlign = align;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setDataTextPosition(PositionType type) {
        this.positionType = type;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setDataTextDeltaX(double dataTextDeltaX) {
        this.dataTextDeltaX = dataTextDeltaX;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setDataTextDeltaY(double dataTextDeltaY) {
        this.dataTextDeltaY = dataTextDeltaY;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setDataTextStyle(TextStyle dataTextStyle) {
        this.textStyle = dataTextStyle;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setShowDataTextProcessed(boolean dataTextProcessed) {
        this.dataTextProcessed = dataTextProcessed;
        return (S) this;
    }

    @Override
    public String getXmlElementName() {
        return "Barcode";
    }

    public void exportBarcodeProperties(XmlExporter exporter, String barcodeName) {
        exporter.addElementWithStringData("BarcodeName", barcodeName)
                .addElementWithStringData("ConvertString", data)
                .addElementWithIface("VariableId", variable)
                .addElementWithIface("FillStyleId", barcodeFillStyle)
                .addElementWithIface("FillBackgroungStyleId", backgroundFillStyle)
                .addElementWithBoolData("UseCodec", useEncoding)
                .addElementWithStringData("TextCodec", encoding)
                .addElementWithStringData("SettingsLocation", settingsLocation)
                .addElementWithStringData("HorizontalAlign", convertHorizontalAlignToXmlName(horizontalAlign))
                .addElementWithStringData("VerticalAlign", convertVerticalAlignToXmlName(verticalAlign))
                .addElementWithStringData("DataTextPosition", convertPositionTypeToXmlName(positionType))
                .addElementWithDoubleData("DataTextDeltaX", dataTextDeltaX)
                .addElementWithDoubleData("DataTextDeltaY", dataTextDeltaY)
                .addElementWithIface("TextStyleId", textStyle)
                .addElementWithBoolData("ShowDataTextProcessed", dataTextProcessed);
    }
}
