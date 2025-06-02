package com.quadient.wfdxml.internal.data.converters

import com.quadient.wfdxml.api.data.converters.CustCodeFieldConvertor
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEquals

class CustStr2StrFCVTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "export empty"() {
        given:
        CustStr2StrFCV cust = new CustStr2StrFCV()

        when:
        cust.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
            <FCV FCVClassName="CustStr2StrFCV">
                <FCVProps>
                    <InputType>String</InputType>
                    <OutputType>String</OutputType>
                    <DefaultValue>Default</DefaultValue>
                    <PassThrough>False</PassThrough>
                </FCVProps>
            </FCV>
           """)
    }

    def "export all values set"() {
        given:
        CustCodeFieldConvertor cust = new CustStr2StrFCV()
        cust
                .setDefaultValue("Test default value")
                .setPassThrough(true)
                .addCase("In 1", "Out 1")
                .addCase("In 2", "Out 2\nLine 2")

        when:
        cust.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
            <FCV FCVClassName="CustStr2StrFCV">
                <FCVProps>
                    <InputType>String</InputType>
                    <OutputType>String</OutputType>
                    <DefaultValue>Test default value</DefaultValue>
                    <PassThrough>True</PassThrough>
                    <Field Key="In 1" Value="Out 1"/>
                    <Field Key="In 2" Value="Out 2\nLine 2"/>
                </FCVProps>
            </FCV>
           """)

    }
}