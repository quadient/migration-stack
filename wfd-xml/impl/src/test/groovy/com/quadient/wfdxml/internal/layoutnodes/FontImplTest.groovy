package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.LocationType
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class FontImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "font serialization"() {
        given:
        FontImpl font = new FontImpl()
        font.addSubfont()

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
        FontImpl font = new FontImpl().setName("Gigi").setFontName("Gigi")
        font.addSubfont().setBold(true).setItalic(true).setLocation("Gigi", LocationType.FONT)

        when:
        font.export(exporter)

        then:
        String expected = """ 
            <FontName>Gigi</FontName>
            <SubFont Name="Bold Italic" Bold="True" Italic="True">
                <FontIndex>0</FontIndex>
                <FontLocation>FONT_DIR,Gigi.TTF</FontLocation>
            </SubFont>"""

        assertXmlEqualsWrapRoot(exporter.buildString(), expected)
    }

    def "font disk location font with specific name"() {
        when:
        FontImpl font = new FontImpl()
                .setName("My Custom Font Name") as FontImpl
        font.addSubfont().setLocation("C:/test directory/test font.ttf", LocationType.DISK)

        then:
        assert font.name == "My Custom Font Name"
    }

    def "def font name"() {
        when:
        FontImpl font = new FontImpl()

        then:
        assert font.name == "Arial"
    }
}