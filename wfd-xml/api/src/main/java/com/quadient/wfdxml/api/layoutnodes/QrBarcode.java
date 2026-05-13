package com.quadient.wfdxml.api.layoutnodes;

public interface QrBarcode extends Barcode<QrBarcode> {
    QrBarcode setModuleWidth(double moduleWidth);

    QrBarcode setWhiteSpace(double whiteSpace);

    QrBarcode setBarcodeSize(int sizeEnum);

    QrBarcode setEciCode(int eciCodeEnum);

    QrBarcode setErrorCorrection(int errorCorrectionEnum);
}
