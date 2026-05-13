package com.quadient.wfdxml.api.layoutnodes;

public interface BarcodeFactory {

    DataMatrixBarcode addDataMatrix();
    QrBarcode addQr();
    Code39Barcode addCode39();
}
