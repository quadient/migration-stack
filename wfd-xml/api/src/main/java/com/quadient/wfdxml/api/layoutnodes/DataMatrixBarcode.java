package com.quadient.wfdxml.api.layoutnodes;

public interface DataMatrixBarcode extends Barcode<DataMatrixBarcode> {

    DataMatrixBarcode setModuleWidth(double moduleWidth);

    DataMatrixBarcode setQuitZoneRatio(int ratioIndex);

    DataMatrixBarcode setWiziwayBarcode(boolean wiziwayBarcode);

    DataMatrixBarcode setUsePNet3Encoding(boolean usePNet3Encoding);

    DataMatrixBarcode setDataMatrixEncoding(DataMatrixEncodingType encoding);

    DataMatrixBarcode setUseTildaCharacter(boolean useTildaCharacter);

    enum DataMatrixEncodingType {
        AUTODETECT,
        ASCII,
        BASE11,
        BASE41,
        BASE256
    }
}
