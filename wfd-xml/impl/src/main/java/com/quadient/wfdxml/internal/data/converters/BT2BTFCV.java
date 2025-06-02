package com.quadient.wfdxml.internal.data.converters;

import com.quadient.wfdxml.api.data.converters.DoubleFieldConverter;
import com.quadient.wfdxml.api.data.converters.IntFieldConverter;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

/**
 * Name is from designer i guess that it means "BasicType To BasicType FieldConvertor"
 */
public class BT2BTFCV extends FieldConverterImpl<BT2BTFCV> implements DoubleFieldConverter, IntFieldConverter {

    private boolean doubleSkipWarnings = true;
    private double doubleEmptyResult = 0.0d;
    private double doubleErrorResult = 0.0d;
    private boolean doubleSkipErrors = false;

    private boolean doubleIgnoreCharacters = false;
    private String doubleIgnoreThese = "";
    private int doubleNegativeFormat = 0;
    private String doubleDecimalPoint = ".";
    private String separateThousandsWith = ",";
    private boolean checkCompleteParsing = true;

    public boolean isDoubleSkipWarnings() {
        return doubleSkipWarnings;
    }

    public BT2BTFCV setDoubleSkipWarnings(boolean doubleSkipWarnings) {
        this.doubleSkipWarnings = doubleSkipWarnings;
        return this;
    }

    @Override
    public BT2BTFCV setSkipWarningsWithEmptyData(boolean skip) {
        return setDoubleSkipWarnings(skip);
    }

    public double getDoubleEmptyResult() {
        return doubleEmptyResult;
    }

    public BT2BTFCV setDoubleEmptyResult(double doubleEmptyResult) {
        this.doubleEmptyResult = doubleEmptyResult;
        return this;
    }

    @Override
    public BT2BTFCV setEmptyInputResult(double emptyInputResult) {
        return setDoubleEmptyResult(emptyInputResult);
    }

    public double getDoubleErrorResult() {
        return doubleErrorResult;
    }

    public BT2BTFCV setDoubleErrorResult(double doubleErrorResult) {
        this.doubleErrorResult = doubleErrorResult;
        return this;
    }

    @Override
    public BT2BTFCV setWrongInputResult(double wrongInputResult) {
        return setDoubleErrorResult(wrongInputResult);
    }

    @Override
    public IntFieldConverter setWrongInputResult(long wrongInputResult) {
        return setWrongInputResult((double) wrongInputResult);
    }

    public boolean isDoubleSkipErrors() {
        return doubleSkipErrors;
    }

    public BT2BTFCV setDoubleSkipErrors(boolean doubleSkipErrors) {
        this.doubleSkipErrors = doubleSkipErrors;
        return this;
    }

    @Override
    public BT2BTFCV setSkipWarningsWithErrorData(boolean skip) {
        return setDoubleSkipErrors(skip);
    }

    public boolean isDoubleIgnoreCharacters() {
        return doubleIgnoreCharacters;
    }


    public BT2BTFCV setDoubleIgnoreCharacters(boolean doubleIgnoreCharacters) {
        this.doubleIgnoreCharacters = doubleIgnoreCharacters;
        return this;
    }

    @Override
    public BT2BTFCV setIgnoreCharacters(boolean ignoreCharacters) {
        return setDoubleIgnoreCharacters(true);
    }

    public String getDoubleIgnoreThese() {
        return doubleIgnoreThese;
    }

    public BT2BTFCV setDoubleIgnoreThese(String doubleIgnoreThese) {
        this.doubleIgnoreThese = doubleIgnoreThese;
        return this;
    }

    @Override
    public BT2BTFCV setIgnoreThese(String ignoreThese) {
        return setDoubleIgnoreThese(ignoreThese);
    }

    public int getDoubleNegativeFormat() {
        return doubleNegativeFormat;
    }

    public BT2BTFCV setDoubleNegativeFormat(int doubleNegativeFormat) {
        this.doubleNegativeFormat = doubleNegativeFormat;
        return this;
    }

    @Override
    public BT2BTFCV setNegativeFormat(NegativeFormat negativeFormat) {
        switch (negativeFormat) {
            case SIGN_AT_BEGIN:
                doubleNegativeFormat = 0;
                break;
            case ROUND_BRACKET:
                doubleNegativeFormat = 1;
                break;
            case SIGN_AT_END:
                doubleNegativeFormat = 2;
                break;
            default:
                throw new IllegalArgumentException(negativeFormat.toString());
        }
        return this;
    }

    public String getDoubleDecimalPoint() {
        return doubleDecimalPoint;
    }

    public BT2BTFCV setDoubleDecimalPoint(String doubleDecimalPoint) {
        this.doubleDecimalPoint = doubleDecimalPoint;
        return this;
    }

    @Override
    public BT2BTFCV setDecimalPoint(String doubleDecimalPoint) {
        return setDoubleDecimalPoint(doubleDecimalPoint);
    }

    public String getSeparateThousandsWith() {
        return separateThousandsWith;
    }

    public BT2BTFCV setSeparateThousandsWith(String separateThousandsWith) {
        this.separateThousandsWith = separateThousandsWith;
        return this;
    }

    @Override
    public BT2BTFCV setThousandsSeparator(String separateThousandsWith) {
        return setSeparateThousandsWith(separateThousandsWith);
    }


    @Override
    public BT2BTFCV setEmptyInputResult(long emptyInputResult) {
        return setDoubleEmptyResult(emptyInputResult);
    }

    public boolean isCheckCompleteParsing() {
        return checkCompleteParsing;
    }

    public BT2BTFCV setCheckCompleteParsing(boolean checkCompleteParsing) {
        this.checkCompleteParsing = checkCompleteParsing;
        return this;
    }

    @Override
    String getFieldConverterClassNameForExport() {
        return "BT2BTFCV";
    }

    @Override
    void exportFieldConverterSettings(XmlExporter exporter) {
        switch (getInputType()) {
            case STRING: {
                switch (getOutputType()) {
                    case CURRENCY:
                    case DOUBLE:
                        exporter.addElementWithBoolData("SkipWarnings", doubleSkipWarnings);
                        exporter.addElementWithBoolData("IgnoreCharacters", doubleIgnoreCharacters);
                        exporter.addElementWithStringData("IgnoreThese", doubleIgnoreThese);
                        switch (doubleNegativeFormat) {
                            case 0:
                                exporter.addElementWithStringData("NegativeFormat", "SignAtBegin");
                                break;
                            case 1:
                                exporter.addElementWithStringData("NegativeFormat", "RoundBracket");
                                break;
                            case 2:
                                exporter.addElementWithStringData("NegativeFormat", "SignAtEnd");
                                break;
                        }
                        exporter.addElementWithStringData("DecimalPoint", doubleDecimalPoint);
                        exporter.addElementWithDoubleData("EmptyResult", doubleEmptyResult);
                        exporter.addElementWithDoubleData("ErrorResult", doubleErrorResult);
                        exporter.addElementWithBoolData("SkipErrors", doubleSkipErrors);
                        exporter.addElementWithStringData("SeparateThousandsWith", separateThousandsWith);
                        exporter.addElementWithBoolData("CheckCompleteParsing", checkCompleteParsing);
                        break;
                    case INT:
                    case INT64:
                        exporter.addElementWithBoolData("SkipWarnings", doubleSkipWarnings);
                        exporter.addElementWithDoubleData("EmptyResult", doubleEmptyResult);
                        exporter.addElementWithDoubleData("ErrorResult", doubleErrorResult);
                        exporter.addElementWithBoolData("SkipErrors", doubleSkipErrors);
                        break;
                    case BOOL:
                        break;
                    default:
                        throw new UnsupportedOperationException("Combination Input/Output type '" +
                                getInputType() + "/" + getOutputType() + "' is not supported");
                }
            }
            break;
            default:
                throw new UnsupportedOperationException("Input type '" + getInputType() + "' is not supported");
        }
    }
}
