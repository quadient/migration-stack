package com.quadient.wfdxml.internal.data.converters

import spock.lang.Specification

import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.DateOption
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.ErrorLevel
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.ParsingMethod
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.TimeOption

class ToDateTimeFCV_EnumConversionsTest extends Specification {
    def "test timeOptionToExportString"() {
        expect:
        ToDateTimeFCV.timeOptionToExportString(timeOption) == expectedResult

        where:
        expectedResult | _
        "None"         | _
        "Full"         | _
        "Long"         | _
        "Medium"       | _
        "Short"        | _

        timeOption << TimeOption.values().toList()
    }

    def "test dateOptionToExportString"() {
        expect:
        ToDateTimeFCV.dateOptionToExportString(dateOption) == expectedResult

        where:
        expectedResult | _
        "None"         | _
        "Full"         | _
        "Long"         | _
        "Medium"       | _
        "Short"        | _
        "DateOffset"   | _
        "DateTime"     | _

        dateOption << DateOption.values().toList()
    }

    def "test parsingMethodToExportString"() {
        expect:
        ToDateTimeFCV.parsingMethodToExportString(parsingMethod) == expectedResult

        where:
        expectedResult | _
        "TypeOfOutput" | _
        "UserPattern"  | _

        parsingMethod << ParsingMethod.values().toList()
    }

    def "test errorReportingToExportString"() {
        expect:
        ToDateTimeFCV.errorReportingToExportString(errorLevel) == expectedResult

        where:
        expectedResult | _
        "Warning"      | _
        "Error"        | _

        errorLevel << ErrorLevel.values().toList()
    }
}