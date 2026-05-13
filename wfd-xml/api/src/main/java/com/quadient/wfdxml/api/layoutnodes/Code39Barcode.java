package com.quadient.wfdxml.api.layoutnodes;

public interface Code39Barcode extends Barcode<Code39Barcode> {

    Code39Barcode setModuleWidth(double moduleWidth);

    Code39Barcode setBarcodeHeight(double height);

    Code39Barcode setWhiteSpace(double whiteSpace);

    Code39Barcode setRatio(double ratio);

    Code39Barcode setInterCharacterSpaceRatio(double ratio);

    Code39Barcode narrowBarWidthCorrection(double width);

    Code39Barcode wideBarWidthCorrection(double width);

    Code39Barcode setDirectMetric(boolean value);

    Code39Barcode setFirstBarWidth(double width);

    Code39Barcode setSecondBarWidth(double width);

    Code39Barcode setFirstBarSpace(double width);

    Code39Barcode secondBarSpace(double width);

    Code39Barcode useControlSum(boolean value);
}
