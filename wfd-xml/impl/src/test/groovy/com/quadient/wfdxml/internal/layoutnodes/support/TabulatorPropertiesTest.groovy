package com.quadient.wfdxml.internal.layoutnodes.support

import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.CENTER
import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.DECIMAL
import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.WORD_DECIMAL
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class TabulatorPropertiesTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "empty tabulatorProperties serialization"() {
        given:
        TabulatorProperties tabulatorProperties = new TabulatorProperties()

        when:
        tabulatorProperties.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <TabulatorProperties>
                    <Default>0.0125</Default>
                    <UseOutsideTabs>False</UseOutsideTabs>
                </TabulatorProperties>""")
    }

    def "tabulatorProperties set all properties serialization"() {
        given:
        TabulatorProperties tabulatorProperties = new TabulatorProperties()
                .setUseOutsideTabs(true)
                .setDefaultTab(0.1005d)
                .addTabulator((double) 0.0035, CENTER)

        when:
        tabulatorProperties.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <TabulatorProperties>
                    <Default>0.1005</Default>
                    <UseOutsideTabs>True</UseOutsideTabs>
                    <Tabulator>
                        <Type>Center</Type>
                        <Pos>0.0035</Pos>
                    </Tabulator>
                </TabulatorProperties>""")
    }

    def "tabulatorProperties add Tabulators serialization"() {
        given:
        TabulatorProperties tabulatorProperties = new TabulatorProperties()
                .addTabulator((double) 0.0035, DECIMAL)
                .addTabulator((double) 0.01234, WORD_DECIMAL)

        when:
        tabulatorProperties.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <TabulatorProperties>
                    <Default>0.0125</Default>
                    <UseOutsideTabs>False</UseOutsideTabs>
                    <Tabulator>
                        <Type>Decimal</Type>
                        <Pos>0.0035</Pos>
                    </Tabulator>
                    <Tabulator>
                        <Type>WordDecimal</Type>
                        <Pos>0.01234</Pos>
                    </Tabulator>
                </TabulatorProperties>""")
    }

    def "tabulatorProperties add Tabulator and Tabulators with point serialization"() {
        given:
        TabulatorProperties tabulatorProperties = new TabulatorProperties()
                .addTabulator(0.0035d, DECIMAL)
                .addDecimalTabulatorWithPoint(0.01234d, ",")

        when:
        tabulatorProperties.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <TabulatorProperties>
                    <Default>0.0125</Default>
                    <UseOutsideTabs>False</UseOutsideTabs>
                    <Tabulator>
                        <Type>Decimal</Type>
                        <Pos>0.0035</Pos>
                    </Tabulator>
                    <Tabulator>
                        <Type>Decimal</Type>
                        <Separator>,</Separator>
                        <Pos>0.01234</Pos>
                    </Tabulator>
                </TabulatorProperties>""")
    }
}
