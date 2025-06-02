package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.DataMatrixBarcode;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class DataMatrixBarcodeImpl extends BarcodeImpl<DataMatrixBarcode> implements DataMatrixBarcode, XmlExportable {
    private static final String TYPE = "DataMatrixBarcodeGenerator";
    private static final String BARCODE_NAME = "Data Matrix";
    private final double symbolSizeIndex = 0.0;
    private DataMatrixEncodingType dataMatrixEncodingType = DataMatrixEncodingType.AUTODETECT;
    private double moduleWidth;
    private double whiteSpace;
    private boolean wiziwayMode = false;
    private boolean usePnet3Encoding = true;
    private boolean useDriveCharacter = true;

    public static String convertEncodingTypeToXmlName(DataMatrixEncodingType type) {
        switch (type) {
            case AUTODETECT:
                return "Autodetect";
            case ASCII:
                return "Ascii";
            case BASE11:
                return "Base11";
            case BASE41:
                return "Base41";
            case BASE256:
                return "Base256";
            default:
                throw new IllegalStateException(type.toString());

        }
    }

    public static String getBarcodeName() {
        return BARCODE_NAME;
    }

    public static String getTYPE() {
        return TYPE;
    }

    @Override
    public DataMatrixBarcode setModuleWidth(double moduleWidth) {
        this.moduleWidth = moduleWidth;
        return this;
    }

    @Override
    public DataMatrixBarcodeImpl setQuitZoneRatio(int ratioIndex) {
        this.whiteSpace = ratioIndex;
        return this;
    }

    @Override
    public DataMatrixBarcodeImpl setWiziwayBarcode(boolean wiziwayBarcode) {
        this.wiziwayMode = wiziwayBarcode;
        return this;
    }

    @Override
    public DataMatrixBarcodeImpl setUsePNet3Encoding(boolean usePNet3Encoding) {
        this.usePnet3Encoding = usePNet3Encoding;
        return this;
    }

    @Override
    public DataMatrixBarcodeImpl setDataMatrixEncoding(DataMatrixEncodingType encoding) {
        this.dataMatrixEncodingType = encoding;
        return this;
    }

    @Override
    public DataMatrixBarcodeImpl setUseTildaCharacter(boolean useTildaCharacter) {
        this.useDriveCharacter = useTildaCharacter;
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
                .addElementWithBoolData("WiziwayMode", wiziwayMode)
                .addElementWithBoolData("UsePnet3Encoding", usePnet3Encoding)
                .addElementWithDoubleData("SymbolSizeIndex", symbolSizeIndex)
                .addElementWithStringData("CompressType", convertEncodingTypeToXmlName(dataMatrixEncodingType))
                .addElementWithBoolData("UseDriveCharacter", useDriveCharacter)
                .endElement();
    }

    public double getModuleWidth() {
        return moduleWidth;
    }

    public double getWhiteSpace() {
        return whiteSpace;
    }

    public boolean isWiziwayMode() {
        return wiziwayMode;
    }

    public boolean isUsePnet3Encoding() {
        return usePnet3Encoding;
    }

    public double getSymbolSizeIndex() {
        return symbolSizeIndex;
    }

    public DataMatrixEncodingType getDataMatrixEncodingType() {
        return dataMatrixEncodingType;
    }

    public boolean isUseDriveCharacter() {
        return useDriveCharacter;
    }
}
