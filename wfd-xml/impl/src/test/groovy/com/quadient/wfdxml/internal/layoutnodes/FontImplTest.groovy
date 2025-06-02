package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class FontImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "font serialization"() {
        given:
        FontImpl font = new FontImpl()

        when:
        font.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <SubFont Name="Regular" Bold="False" Italic="False">
                <FontIndex>0</FontIndex>
                <FontLocation>FONT_DIR,Arial.TTF</FontLocation>
            </SubFont> """)
    }

    def "font allSet serialization"() {
        given:
        FontImpl font = new FontImpl()
                .setFont("Gigi")
                .setBold(true)
                .setItalic(true)
        when:
        font.export(exporter)

        then:
        String expected = """ 
            <SubFont Name="Regular" Bold="True" Italic="True">
                <FontIndex>0</FontIndex>
                <FontLocation>FONT_DIR,Gigi.TTF</FontLocation>
            </SubFont> """

        assertXmlEqualsWrapRoot(exporter.buildString(), expected)
    }

    def "font disk location font with specific name"() {
        when:
        FontImpl font = new FontImpl()
                .setFontFromDiskLocation("C:/test directory/test font.ttf")
                .setName("My Custom Font Name") as FontImpl

        then:
        assert font.name == "My Custom Font Name"
    }

    def "font from disk location font use has font name extracted from file name"() {
        when:
        FontImpl font = new FontImpl()
                .setFontFromDiskLocation("C:/test directory/test font.ttf")

        then:
        assert font.name == "test font.ttf"
    }

    def "def font exist "() {
        when:
        FontImpl font = new FontImpl()

        then:
        assert font.getSubFont().getFontLocation() == "FONT_DIR,Arial.TTF"
    }

    def "def font name"() {
        when:
        FontImpl font = new FontImpl()

        then:
        assert font.name == "Arial"
    }
}