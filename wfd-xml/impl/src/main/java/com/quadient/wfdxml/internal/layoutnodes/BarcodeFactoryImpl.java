package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.BarcodeFactory;
import com.quadient.wfdxml.api.layoutnodes.Code39Barcode;
import com.quadient.wfdxml.api.layoutnodes.QrBarcode;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.internal.NodeImpl;

import java.util.List;

public class BarcodeFactoryImpl implements BarcodeFactory {
    private final List<NodeImpl> children;
    private final TextStyle defTextStyle;

    public BarcodeFactoryImpl(List<NodeImpl> childrenList, TextStyle textStyle) {
        this.children = childrenList;
        this.defTextStyle = textStyle;
    }

    @Override
    public DataMatrixBarcodeImpl addDataMatrix() {
        DataMatrixBarcodeImpl barcode = new DataMatrixBarcodeImpl();
        barcode.setDataTextStyle(defTextStyle);
        children.add(barcode);
        return barcode;
    }

    @Override
    public QrBarcode addQr() {
        QrBarcodeImpl barcode = new QrBarcodeImpl();
        barcode.setDataTextStyle(defTextStyle);
        children.add(barcode);
        return barcode;
    }

    @Override
    public Code39Barcode addCode39() {
        Code39BarcodeImpl barcode = new Code39BarcodeImpl();
        barcode.setDataTextStyle(defTextStyle);
        children.add(barcode);
        return barcode;
    }
}
