package com.quadient.wfdxml.internal.module.textreplacer

import com.quadient.wfdxml.api.module.TextReplacer
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class TextReplacerImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "textReplacer serialization"() {
        given:
        TextReplacerImpl replacer = new TextReplacerImpl()

        when:
        replacer.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                 <TextReplacer>
                    <Id>SR_1</Id>
                    <ModulePos X="0" Y="0"></ModulePos>
                 </TextReplacer>
                """)
    }

    def "textReplacer allSet"() {
        given:
        TextReplacer replacer = new TextReplacerImpl()
        replacer.setPosX(15).setPosY(-23).setName("Module")

        when:
        replacer.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                  <TextReplacer>
                    <Id>SR_1</Id>
                    <Name>Module</Name>
                    <ModulePos X="15" Y="-23"></ModulePos>
                  </TextReplacer>
                """)
    }
}