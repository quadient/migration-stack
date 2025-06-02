package com.quadient.wfdxml.api.data.converters;

public interface DateTimeFieldConverter extends FieldConverter {

    DateTimeFieldConverter setLocale(String locale);

    DateTimeFieldConverter setParsingMethod(ParsingMethod parsingMethod);

    DateTimeFieldConverter setRecognizePattern(String recognizePattern);

    DateTimeFieldConverter setTimeOption(TimeOption timeOption);

    DateTimeFieldConverter setDateOption(DateOption dateOption);

    DateTimeFieldConverter setLenient(boolean lenient);

    DateTimeFieldConverter setErrorLevel(ErrorLevel errorLevel);

    DateTimeFieldConverter setIgnoreEmptyString(boolean ignoreEmptyString);

    DateTimeFieldConverter setShowMessageOnce(boolean showMessageOnce);

    enum TimeOption {
        NONE,
        FULL,
        LONG,
        MEDIUM,
        SHORT,
    }

    enum DateOption {
        NONE,
        FULL,
        LONG,
        MEDIUM,
        SHORT,
        DATE_OFFSET,
        DATE_TIME,
    }

    enum ParsingMethod {
        TYPE_OF_INPUT,
        USER_PATTERN,
    }

    enum ErrorLevel {
        WARNING,
        ERROR,
    }
}