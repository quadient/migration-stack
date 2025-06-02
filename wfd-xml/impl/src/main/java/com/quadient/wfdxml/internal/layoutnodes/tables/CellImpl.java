package com.quadient.wfdxml.internal.layoutnodes.tables;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.api.layoutnodes.tables.Cell;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.CellVerticalAlignment.TOP;
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.FittingType.NONE;
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.HTMLWidthType.AUTO;

public class CellImpl extends NodeImpl<Cell> implements Cell {

    private Flow flow;
    private BorderStyle border = null;
    private double minWidth = 0.001;
    private double maxWidth = 300.0;
    private double ratioWidth = 1;
    private double minHeight = 0;
    private double maxHeight = 300;
    private boolean flowToNextPage = false;
    private boolean spanLeft = false;
    private boolean spanUp = false;
    private CellVerticalAlignment cVA = TOP;
    private FittingType fittingType = NONE;
    private boolean withFixedHeight = false;
    private boolean alwaysProcess = true;
    private boolean relativeFill = true;
    private HTMLWidthType htmlWidthType = AUTO;
    private double htmlWidthValue = 100;

    public Flow getFlow() {
        return flow;
    }

    @Override
    public CellImpl setFlow(Flow flow) {
        this.flow = flow;
        return this;
    }

    public BorderStyle getBorder() {
        return border;
    }

    public CellImpl setBorder(BorderStyle border) {
        this.border = border;
        return this;
    }

    @Override
    public CellImpl setBorderStyle(BorderStyle borderStyle) {
        return setBorder(borderStyle);
    }

    public double getMinWidth() {
        return minWidth;
    }

    public CellImpl setMinWidth(double minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    public CellImpl setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public double getRatioWidth() {
        return ratioWidth;
    }

    public CellImpl setRatioWidth(double ratioWidth) {
        this.ratioWidth = ratioWidth;
        return this;
    }

    public double getMinHeight() {
        return minHeight;
    }

    @Override
    public CellImpl setMinHeight(double minHeight) {
        this.minHeight = minHeight;
        return this;
    }

    @Override
    public Cell setFixedHeight(double fixedHeight) {
        return setMinHeight(fixedHeight); //Designer save this in MinHeight
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    @Override
    public CellImpl setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    public boolean isFlowToNextPage() {
        return flowToNextPage;
    }

    @Override
    public CellImpl setFlowToNextPage(boolean flowToNextPage) {
        this.flowToNextPage = flowToNextPage;
        return this;
    }


    public boolean isSpanLeft() {
        return spanLeft;
    }

    @Override
    public CellImpl setSpanLeft(boolean spanLeft) {
        this.spanLeft = spanLeft;
        return this;
    }

    public boolean isSpanUp() {
        return spanUp;
    }

    @Override
    public CellImpl setSpanUp(boolean spanUp) {
        this.spanUp = spanUp;
        return this;
    }

    public CellVerticalAlignment getcVA() {
        return cVA;
    }

    public CellImpl setcVA(CellVerticalAlignment cVA) {
        this.cVA = cVA;
        return this;
    }

    @Override
    public Cell setAlignment(CellVerticalAlignment alignment) {
        return setcVA(alignment);
    }

    public FittingType getFittingType() {
        return fittingType;
    }

    public CellImpl setFittingType(FittingType fittingType) {
        this.fittingType = fittingType;
        return this;
    }

    @Override
    public Cell setFitting(FittingType fitting) {
        return setFittingType(fitting);
    }

    @Override
    public Cell setType(CellType type) {
        switch (type) {
            case CUSTOM:
                return setWithFixedHeight(false);
            case FIXED_HEIGHT:
                return setWithFixedHeight(true);
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    public boolean isWithFixedHeight() {
        return withFixedHeight;
    }

    public CellImpl setWithFixedHeight(boolean withFixedHeight) {
        this.withFixedHeight = withFixedHeight;
        return this;
    }

    public boolean isAlwaysProcess() {
        return alwaysProcess;
    }

    public CellImpl setAlwaysProcess(boolean alwaysProcess) {
        this.alwaysProcess = alwaysProcess;
        return this;
    }

    public boolean isRelativeFill() {
        return relativeFill;
    }

    public CellImpl setRelativeFill(boolean relativeFill) {
        this.relativeFill = relativeFill;
        return this;
    }

    @Override
    public Cell setFillRelativeToCell(boolean fillRelativeToCell) {
        return setRelativeFill(fillRelativeToCell);
    }

    public HTMLWidthType getHtmlWidthType() {
        return htmlWidthType;
    }

    public CellImpl setHtmlWidthType(HTMLWidthType htmlWidthType) {
        this.htmlWidthType = htmlWidthType;
        return this;
    }

    @Override
    public Cell setHtmlWidth(HTMLWidthType htmlWidth) {
        return setHtmlWidthType(htmlWidth);
    }

    public double getHtmlWidthValue() {
        return htmlWidthValue;
    }

    public CellImpl setHtmlWidthValue(double htmlWidthValue) {
        this.htmlWidthValue = htmlWidthValue;
        return this;
    }

    @Override
    public Cell setFixedWidth(double fixedWidth) {
        return setHtmlWidthValue(fixedWidth);
    }

    @Override
    public Cell setWidthInPercent(double widthInPercent) {
        return setHtmlWidthValue(widthInPercent);
    }

    @Override
    public String getXmlElementName() {
        return "Cell";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter
                .addElementWithIface("FlowId", flow)
                .addElementWithIface("BorderId", border)
                .addElementWithBoolData("WithFixedHeight", withFixedHeight)
                .addElementWithBoolData("AlwaysProcess", alwaysProcess)
                .addElementWithDoubleData("MinWidth", minWidth)
                .addElementWithDoubleData("MaxWidth", maxWidth)
                .addElementWithDoubleData("RatioWidth", ratioWidth)
                .addElementWithDoubleData("MinHeight", minHeight)
                .addElementWithDoubleData("MaxHeight", maxHeight)
                .addElementWithBoolData("FlowToNextPage", flowToNextPage)
                .addElementWithBoolData("SpanLeft", spanLeft)
                .addElementWithBoolData("SpanUp", spanUp)
                .addElementWithStringData("CellVerticalAlignment", cVAToXml(cVA))
                .addElementWithStringData("FittingType", fittingToXml(fittingType))
                .addElementWithBoolData("RelativeFill", relativeFill)
                .addElementWithStringData("HtmlWidthType", htmlWidthToXml(htmlWidthType))
                .addElementWithDoubleData("HtmlWidthValue", htmlWidthValue);
    }


    private String cVAToXml(CellVerticalAlignment cVA) {
        switch (cVA) {
            case TOP:
                return "Top";
            case BOTTOM:
                return "Bottom";
            case CENTER:
                return "Center";
            default:
                throw new IllegalStateException(cVA.toString());
        }
    }

    private String fittingToXml(FittingType fittingType) {
        switch (fittingType) {
            case NONE:
                return "None";
            case BOTH:
                return "Both";
            case HORIZONTAL:
                return "Horizontal";
            case VERTICAL:
                return "Vertical";
            default:
                throw new IllegalStateException(fittingType.toString());
        }
    }

    private String htmlWidthToXml(HTMLWidthType htmlWidthType) {
        switch (htmlWidthType) {
            case AUTO:
                return "Auto";
            case LENGTH:
                return "Length";
            case PERCENT:
                return "Percent";
            default:
                throw new IllegalStateException(htmlWidthType.toString());
        }
    }

}
