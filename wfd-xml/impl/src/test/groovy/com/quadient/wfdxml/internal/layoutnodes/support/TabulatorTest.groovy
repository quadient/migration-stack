package com.quadient.wfdxml.internal.layoutnodes.support

import com.quadient.wfdxml.api.layoutnodes.TabulatorType
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.LEFT
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class TabulatorTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "tabulator serialization"() {
        given:
        Tabulator tabulator = new Tabulator(0.0035d, LEFT)

        when:
        tabulator.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                    <Tabulator>
                        <Type>Left</Type>
                        <Pos>0.0035</Pos>
                    </Tabulator>""")
    }

    def "tabulator with point serialization"() {
        given:
        Tabulator tabulator = new Tabulator(0.0035d, ",")

        when:
        tabulator.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                    <Tabulator>
                        <Type>Decimal</Type>
                         <Separator>,</Separator>
                        <Pos>0.0035</Pos>
                    </Tabulator>""")
    }

    def "tabulatorType"() {
        when:
        List<String> tabulatorTypes = TabulatorType.values().collect {
            Tabulator tabulator = new Tabulator(0, it)
            tabulator.convertTabulatorTypeToXmlName()
        }

        then:
        assert tabulatorTypes == ["Left",
                                  "Right",
                                  "Center",
                                  "Decimal",
                                  "WordDecimal"]
    }
}