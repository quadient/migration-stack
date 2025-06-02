package com.quadient.wfdxml.api.data.converters;

public interface IntFieldConverter extends FieldConverter {
    IntFieldConverter setEmptyInputResult(long emptyInputResult);

    IntFieldConverter setSkipWarningsWithEmptyData(boolean skipWarningsWithEmptyData);

    IntFieldConverter setWrongInputResult(long wrongInputResult);

    IntFieldConverter setSkipWarningsWithErrorData(boolean skipWarningsWithErrorData);
}