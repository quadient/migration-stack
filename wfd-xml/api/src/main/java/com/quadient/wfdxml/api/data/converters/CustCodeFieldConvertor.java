package com.quadient.wfdxml.api.data.converters;

public interface CustCodeFieldConvertor extends FieldConverter {

    CustCodeFieldConvertor setDefaultValue(String defaultValue);

    CustCodeFieldConvertor setPassThrough(boolean passThrough);

    /**
     * In outputString is new line is written by '\n' character (ASCII code 10)
     */
    CustCodeFieldConvertor addCase(String inputString, String outputString);
}