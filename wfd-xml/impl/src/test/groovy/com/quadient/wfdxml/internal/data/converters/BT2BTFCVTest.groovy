package com.quadient.wfdxml.internal.data.converters

import com.quadient.wfdxml.api.data.converters.Converters
import com.quadient.wfdxml.api.data.converters.DoubleFieldConverter
import com.quadient.wfdxml.api.data.converters.IntFieldConverter
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import com.quadient.wfdxml.utils.AssertXml
import spock.lang.Specification

import static com.quadient.wfdxml.api.data.converters.DoubleFieldConverter.NegativeFormat.ROUND_BRACKET
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.BOOL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DOUBLE

class BT2BTFCVTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export String to Boolean"() {
        given:
        BT2BTFCV bt = new BT2BTFCV().setOutputType(BOOL)

        when:
        bt.export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
            <FCV FCVClassName="BT2BTFCV">
                <FCVProps>
                    <InputType>String</InputType>
                    <OutputType>Bool</OutputType>
                </FCVProps>
            </FCV>          
        """)
    }


    def "export empty Int convertor"() {
        given:
        BT2BTFCV bt = Converters.intConverter() as BT2BTFCV

        when:
        bt.export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
                <FCV FCVClassName="BT2BTFCV">
                    <FCVProps>
                        <InputType>String</InputType>
                        <OutputType>Int</OutputType>
                        <SkipWarnings>True</SkipWarnings>
                        <EmptyResult>0.0</EmptyResult>
                        <ErrorResult>0.0</ErrorResult>
                        <SkipErrors>False</SkipErrors>
                    </FCVProps>
                </FCV>        
            """)
    }

    def "export empty Int64 convertor"() {
        given:
        BT2BTFCV bt = Converters.int64Converter() as BT2BTFCV

        when:
        bt.export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
                <FCV FCVClassName="BT2BTFCV">
                    <FCVProps>
                        <InputType>String</InputType>
                        <OutputType>Int64</OutputType>
                        <SkipWarnings>True</SkipWarnings>
                        <EmptyResult>0.0</EmptyResult>
                        <ErrorResult>0.0</ErrorResult>
                        <SkipErrors>False</SkipErrors>
                    </FCVProps>
                </FCV>        
            """)
    }

    def "export Int convertor with all values set"() {
        given:
        IntFieldConverter c = Converters.intConverter()
                .setEmptyInputResult(88)
                .setSkipWarningsWithEmptyData(false)
                .setWrongInputResult(-61)
                .setSkipWarningsWithErrorData(true)

        when:
        (c as BT2BTFCV).export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
                <FCV FCVClassName="BT2BTFCV">
                    <FCVProps>
                        <InputType>String</InputType>
                        <OutputType>Int</OutputType>
                        <SkipWarnings>False</SkipWarnings>
                        <EmptyResult>88.0</EmptyResult>
                        <ErrorResult>-61.0</ErrorResult>
                        <SkipErrors>True</SkipErrors>
                    </FCVProps>
                </FCV>       
            """)
    }


    def "export String to Double no settings"() {
        given:
        BT2BTFCV bt = new BT2BTFCV().setOutputType(DOUBLE)

        when:
        bt.export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
                <FCV FCVClassName="BT2BTFCV">
                    <FCVProps>
                        <InputType>String</InputType>
                        <OutputType>Double</OutputType>
                        <SkipWarnings>True</SkipWarnings>
                        <IgnoreCharacters>False</IgnoreCharacters>
                        <IgnoreThese/>
                        <NegativeFormat>SignAtBegin</NegativeFormat>
                        <DecimalPoint>.</DecimalPoint>
                        <EmptyResult>0.0</EmptyResult>
                        <ErrorResult>0.0</ErrorResult>
                        <SkipErrors>False</SkipErrors>
                        <SeparateThousandsWith>,</SeparateThousandsWith>
                        <CheckCompleteParsing>True</CheckCompleteParsing>
                    </FCVProps>
                </FCV>     
            """)
    }

    def "export String to Double all values set"() {
        given:
        DoubleFieldConverter dc = Converters.doubleConverter()
                .setIgnoreCharacters(true)
                .setIgnoreThese("Ign")
                .setNegativeFormat(ROUND_BRACKET)
                .setDecimalPoint("'")
                .setThousandsSeparator("_")
                .setEmptyInputResult(0.1)
                .setSkipWarningsWithEmptyData(false)
                .setWrongInputResult(0.2)
                .setSkipWarningsWithErrorData(true)

        when:
        ((BT2BTFCV) dc).export(exporter)

        then:
        AssertXml.assertXmlEquals(exporter.buildString(), """
                <FCV FCVClassName="BT2BTFCV">
                    <FCVProps>
                        <InputType>String</InputType>
                        <OutputType>Double</OutputType>
                        <SkipWarnings>False</SkipWarnings>
                        <IgnoreCharacters>True</IgnoreCharacters>
                        <IgnoreThese>Ign</IgnoreThese>
                        <NegativeFormat>RoundBracket</NegativeFormat>
                        <DecimalPoint>&#39;</DecimalPoint>
                        <EmptyResult>0.1</EmptyResult>
                        <ErrorResult>0.2</ErrorResult>
                        <SkipErrors>True</SkipErrors>
                        <SeparateThousandsWith>_</SeparateThousandsWith>
                        <CheckCompleteParsing>True</CheckCompleteParsing>
                    </FCVProps>
                </FCV>   
            """)
    }
}