package com.quadient.wfdxml.api.data.converters;

public interface DoubleFieldConverter extends FieldConverter {

    DoubleFieldConverter setSkipWarningsWithEmptyData(boolean skip);

    DoubleFieldConverter setEmptyInputResult(double emptyInputResult);

    DoubleFieldConverter setWrongInputResult(double wrongInputResult);

    DoubleFieldConverter setSkipWarningsWithErrorData(boolean skip);

    DoubleFieldConverter setIgnoreCharacters(boolean ignoreCharacters);

    DoubleFieldConverter setIgnoreThese(String ignoreThese);

    DoubleFieldConverter setNegativeFormat(NegativeFormat negativeFormat);

    DoubleFieldConverter setDecimalPoint(String doubleDecimalPoint);

    DoubleFieldConverter setThousandsSeparator(String separateThousandsWith);

    enum NegativeFormat {
        SIGN_AT_BEGIN,
        ROUND_BRACKET,
        SIGN_AT_END
    }
}
