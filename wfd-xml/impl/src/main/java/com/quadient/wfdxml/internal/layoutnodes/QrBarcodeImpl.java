package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.QrBarcode;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class QrBarcodeImpl extends BarcodeImpl<QrBarcode> implements QrBarcode, XmlExportable {
    private static final String TYPE = "QRBarcodeGenerator";
    private static final String BARCODE_NAME = "QR";
    private double moduleWidth;
    private double whiteSpace;
    private Integer barcodeSize;
    private Integer eciCode;
    private int errorCorrection;

    @Override
    public QrBarcode setModuleWidth(double moduleWidth) {
        this.moduleWidth = moduleWidth;
        return this;
    }

    @Override
    public QrBarcode setWhiteSpace(double whiteSpace) {
        this.whiteSpace = whiteSpace;
        return this;
    }

    @Override
    public QrBarcode setBarcodeSize(int sizeEnum) {
        this.barcodeSize = sizeEnum;
        return this;
    }

    @Override
    public QrBarcode setEciCode(int eciCodeEnum) {
        this.eciCode = eciCodeEnum;
        return this;
    }

    @Override
    public QrBarcode setErrorCorrection(int errorCorrectionEnum) {
        this.errorCorrection = errorCorrectionEnum;
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {
        exportLayoutObjectProperties(exporter);
        exportBarcodeProperties(exporter, BARCODE_NAME);
        exporter.beginElement("BarcodeGenerator")
                .addElementWithStringData("Type", TYPE)
                .addElementWithDoubleData("ModulWidth", moduleWidth)
                .addElementWithDoubleData("WhiteSpace", whiteSpace)
                .addElementWithIntData("ErrorLevel", errorCorrection);

        if (barcodeSize != null) {
            exporter.addElementWithIntData("PredefinedBarcodeSize", barcodeSize);
        }

        if (eciCode != null) {
            exporter.addElementWithIntData("ECIcode", eciCode);
        }

        exporter.endElement();
    }
}
