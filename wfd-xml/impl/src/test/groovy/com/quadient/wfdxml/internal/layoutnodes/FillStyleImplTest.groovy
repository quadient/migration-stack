package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class FillStyleImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "fillStyle serialization"() {
        given:
        ColorImpl color = new ColorImpl()
        FillStyleImpl fillStyle = new FillStyleImpl().setColor(color) as FillStyleImpl

        when:
        fillStyle.export(exporter)

        then:
        String expected = """ 
            <ColorId>SR_1</ColorId>
            """
        assertXmlEqualsWrapRoot(exporter.buildString(), expected)
    }
}