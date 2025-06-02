package com.quadient.wfdxml.internal.data.converters

import com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import com.quadient.wfdxml.utils.AssertXml
import spock.lang.Specification

import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.DateOption
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.ErrorLevel.ERROR
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.ParsingMethod.USER_PATTERN
import static com.quadient.wfdxml.api.data.converters.DateTimeFieldConverter.TimeOption

class ToDateTimeFCVTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "export empty"() {
        given:
        ToDateTimeFCV bt = new ToDateTimeFCV()

        when:
        bt.export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
            <FCV FCVClassName="ToDateTimeFCV">
                <FCVProps>
                    <InputType>String</InputType>
                    <OutputType>DateTime</OutputType>
                    <TimeOption>Medium</TimeOption>
                    <DateOption>Medium</DateOption>
                    <Pattern/>
                    <BeLenient>True</BeLenient>
                    <Locale>en</Locale>
                    <FormatMethod>TypeOfOutput</FormatMethod>
                    <ErrorLevel>Warning</ErrorLevel>
                    <IgnoreEmpty>False</IgnoreEmpty>
                    <ShowMessageOnce>False</ShowMessageOnce>
                </FCVProps>
            </FCV>    
        """)
    }

    def "export with all values set"() {
        given:
        DateTimeFieldConverter bt = new ToDateTimeFCV()
        bt
                .setLocale("cs")
                .setParsingMethod(USER_PATTERN)
                .setRecognizePattern("MY_PATTERN")
                .setTimeOption(TimeOption.LONG)
                .setDateOption(DateOption.DATE_OFFSET)
                .setLenient(false)
                .setErrorLevel(ERROR)
                .setIgnoreEmptyString(false)
                .setShowMessageOnce(true)

        when:
        bt.export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
            <FCV FCVClassName="ToDateTimeFCV">
                <FCVProps>
                    <InputType>String</InputType>
                    <OutputType>DateTime</OutputType>
                    <TimeOption>Long</TimeOption>
                    <DateOption>DateOffset</DateOption>
                    <Pattern>MY_PATTERN</Pattern>
                    <BeLenient>False</BeLenient>
                    <Locale>cs</Locale>
                    <FormatMethod>UserPattern</FormatMethod>
                    <ErrorLevel>Error</ErrorLevel>
                    <IgnoreEmpty>False</IgnoreEmpty>
                    <ShowMessageOnce>True</ShowMessageOnce>
                </FCVProps>
            </FCV> 
           """)
    }
}