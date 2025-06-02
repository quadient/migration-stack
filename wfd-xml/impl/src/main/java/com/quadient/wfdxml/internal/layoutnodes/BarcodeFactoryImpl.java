package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.BarcodeFactory;
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
        DataMatrixBarcodeImpl dataMatrixBarcode = new DataMatrixBarcodeImpl();
        dataMatrixBarcode.setDataTextStyle(defTextStyle);
        children.add(dataMatrixBarcode);
        return dataMatrixBarcode;
    }
}
