package com.quadient.wfdxml.internal.data.converters;

import com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.ErrorLevel.WARNING;
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.ParsingMethod.TYPE_OF_INPUT;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DATETIME;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING;

public class ToDateTimeFCV extends FieldConverterImpl<ToDateTimeFCV> implements DateTimeFieldConverter {

    private TimeOption timeOption = TimeOption.MEDIUM;
    private DateOption dateOption = DateOption.MEDIUM;
    private String recognizePattern = "";
    private boolean beLenient = true;
    private String locale = "en";
    private ParsingMethod parsingMethod = TYPE_OF_INPUT;
    private ErrorLevel errorReporting = WARNING;
    private boolean ignoreEmpty = false;
    private boolean showMessageOnce = false;

    public ToDateTimeFCV() {
        super.inputType = STRING;
        super.outputType = DATETIME;
    }

    public static String timeOptionToExportString(TimeOption timeOption) {
        switch (timeOption) {
            case NONE:
                return "None";
            case FULL:
                return "Full";
            case LONG:
                return "Long";
            case MEDIUM:
                return "Medium";
            case SHORT:
                return "Short";
            default:
                throw new IllegalStateException("Illegal timeOption '" + timeOption + "'");
        }
    }

    public static String dateOptionToExportString(DateOption dateOption) {
        switch (dateOption) {
            case NONE:
                return "None";
            case FULL:
                return "Full";
            case LONG:
                return "Long";
            case MEDIUM:
                return "Medium";
            case SHORT:
                return "Short";
            case DATE_OFFSET:
                return "DateOffset";
            case DATE_TIME:
                return "DateTime";
            default:
                throw new IllegalStateException("Illegal dateOption '" + dateOption + "'");
        }
    }

    public static String parsingMethodToExportString(ParsingMethod parsingMethod) {
        switch (parsingMethod) {
            case TYPE_OF_INPUT:
                return "TypeOfOutput";
            case USER_PATTERN:
                return "UserPattern";
            default:
                throw new IllegalStateException("Illegal parsingMethod '" + parsingMethod + "'");
        }
    }

    public static String errorReportingToExportString(ErrorLevel errorReporting) {
        switch (errorReporting) {
            case WARNING:
                return "Warning";
            case ERROR:
                return "Error";
            default:
                throw new IllegalStateException("Illegal errorReporting '" + errorReporting + "'");
        }
    }

    public TimeOption getTimeOption() {
        return timeOption;
    }

    @Override
    public ToDateTimeFCV setTimeOption(TimeOption timeOption) {
        this.timeOption = timeOption;
        return this;
    }

    public DateOption getDateOption() {
        return dateOption;
    }

    @Override
    public ToDateTimeFCV setDateOption(DateOption dateOption) {
        this.dateOption = dateOption;
        return this;
    }

    public String getRecognizePattern() {
        return recognizePattern;
    }

    @Override
    public ToDateTimeFCV setRecognizePattern(String recognizePattern) {
        this.recognizePattern = recognizePattern;
        return this;
    }

    public boolean isBeLenient() {
        return beLenient;
    }

    public ToDateTimeFCV setBeLenient(boolean beLenient) {
        this.beLenient = beLenient;
        return this;
    }

    @Override
    public DateTimeFieldConverter setLenient(boolean lenient) {
        return setBeLenient(lenient);
    }

    public String getLocale() {
        return locale;
    }

    @Override
    public ToDateTimeFCV setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    public ParsingMethod getParsingMethod() {
        return parsingMethod;
    }

    @Override
    public ToDateTimeFCV setParsingMethod(ParsingMethod parsingMethod) {
        this.parsingMethod = parsingMethod;
        return this;
    }

    public ErrorLevel getErrorReporting() {
        return errorReporting;
    }

    public ToDateTimeFCV setErrorReporting(ErrorLevel errorReporting) {
        this.errorReporting = errorReporting;
        return this;
    }

    @Override
    public DateTimeFieldConverter setErrorLevel(ErrorLevel errorLevel) {
        return setErrorReporting(errorLevel);
    }

    public boolean isIgnoreEmpty() {
        return ignoreEmpty;
    }

    public ToDateTimeFCV setIgnoreEmpty(boolean ignoreEmpty) {
        this.ignoreEmpty = ignoreEmpty;
        return this;
    }

    @Override
    public DateTimeFieldConverter setIgnoreEmptyString(boolean ignoreEmptyString) {
        return setIgnoreEmpty(ignoreEmptyString);
    }

    public boolean isShowMessageOnce() {
        return showMessageOnce;
    }

    @Override
    public ToDateTimeFCV setShowMessageOnce(boolean showMessageOnce) {
        this.showMessageOnce = showMessageOnce;
        return this;
    }

    @Override
    public ToDateTimeFCV setOutputType(WorkFlowTreeEnums.NodeType outputType) {
        throw new UnsupportedOperationException("OutputType for ToDateTimeFCV cannot be changed");
    }

    @Override
    public String getFieldConverterClassNameForExport() {
        return "ToDateTimeFCV";
    }

    @Override
    public void exportFieldConverterSettings(XmlExporter exporter) {
        exporter.addElementWithStringData("TimeOption", timeOptionToExportString(timeOption));
        exporter.addElementWithStringData("DateOption", dateOptionToExportString(dateOption));
        exporter.addElementWithStringData("Pattern", recognizePattern);
        exporter.addElementWithBoolData("BeLenient", beLenient);
        exporter.addElementWithStringData("Locale", locale);
        exporter.addElementWithStringData("FormatMethod", parsingMethodToExportString(parsingMethod));
        exporter.addElementWithStringData("ErrorLevel", errorReportingToExportString(errorReporting));
        exporter.addElementWithBoolData("IgnoreEmpty", ignoreEmpty);
        exporter.addElementWithBoolData("ShowMessageOnce", showMessageOnce);
    }
}
