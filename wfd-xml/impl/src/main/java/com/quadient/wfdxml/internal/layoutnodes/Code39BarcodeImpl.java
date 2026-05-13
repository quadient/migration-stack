package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Code39Barcode;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class Code39BarcodeImpl extends BarcodeImpl<Code39Barcode> implements Code39Barcode, XmlExportable {
    private static final String TYPE = "Code39BarcodeGenerator";
    private static final String BARCODE_NAME = "Code 39";
    private double height;
    private double moduleWidth;
    private double whiteSpace;
    private boolean useControlSum;
    private double ratio = 2.0;
    private double interCharacterSpaceRatio = 1.0;
    private double narrowBarWidthCorrection = 0.0;
    private double wideBarWidthCorrection = 0.0;
    private boolean directMetric = false;
    private double firstBarWidth;
    private double secondBarWidth;
    private double firstBarSpace;
    private double secondBarSpace;

    @Override
    public Code39Barcode setModuleWidth(double moduleWidth) {
        this.moduleWidth = moduleWidth;
        return this;
    }

    @Override
    public Code39Barcode setWhiteSpace(double whiteSpace) {
        this.whiteSpace = whiteSpace;
        return this;
    }

    @Override
    public Code39Barcode setRatio(double ratio) {
        this.ratio = ratio;
        return this;
    }

    @Override
    public Code39Barcode setInterCharacterSpaceRatio(double ratio) {
        this.interCharacterSpaceRatio = ratio;
        return this;
    }

    @Override
    public Code39Barcode narrowBarWidthCorrection(double width) {
        this.narrowBarWidthCorrection = width;
        return this;
    }

    @Override
    public Code39Barcode wideBarWidthCorrection(double width) {
        this.wideBarWidthCorrection = width;
        return this;
    }

    @Override
    public Code39Barcode setDirectMetric(boolean value) {
        this.directMetric = value;
        return this;
    }

    @Override
    public Code39Barcode setFirstBarWidth(double width) {
        this.firstBarWidth = width;
        return this;
    }

    @Override
    public Code39Barcode setSecondBarWidth(double width) {
        this.secondBarWidth = width;
        return this;
    }

    @Override
    public Code39Barcode setFirstBarSpace(double width) {
        this.firstBarSpace = width;
        return this;
    }

    @Override
    public Code39Barcode secondBarSpace(double width) {
        this.secondBarSpace = width;
        return this;
    }

    @Override
    public Code39Barcode useControlSum(boolean value) {
        this.useControlSum = value;
        return this;
    }

    @Override
    public Code39Barcode setBarcodeHeight(double height) {
        this.height = height;
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {
        exportLayoutObjectProperties(exporter);
        exportBarcodeProperties(exporter, BARCODE_NAME);
        exporter.beginElement("BarcodeGenerator")
                .addElementWithStringData("Type", TYPE)
                .addElementWithDoubleData("Ratio", ratio)
                .addElementWithDoubleData("Height", height)
                .addElementWithDoubleData("ModulSize", moduleWidth)
                .addElementWithDoubleData("WhiteSpace", whiteSpace)
                .addElementWithBoolData("UseControlSum", useControlSum)
                .addElementWithDoubleData("Bar1", narrowBarWidthCorrection)
                .addElementWithDoubleData("Bar2", wideBarWidthCorrection)
                .addElementWithBoolData("UseDirectMetric", directMetric)
                .addElementWithDoubleData("InterCharacterSpaceRatio", interCharacterSpaceRatio)
                .addElementWithDoubleData("ModuleBlackSize0", firstBarWidth)
                .addElementWithDoubleData("ModuleBlackSize1", secondBarWidth)
                .addElementWithDoubleData("ModuleSpaceSize0", firstBarSpace)
                .addElementWithDoubleData("ModuleSpaceSize1", secondBarSpace);

        exporter.endElement();
    }
}
