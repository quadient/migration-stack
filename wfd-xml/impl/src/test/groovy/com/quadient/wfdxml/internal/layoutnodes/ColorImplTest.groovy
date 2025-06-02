package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Color
import com.quadient.wfdxml.internal.layoutnodes.support.ColorRgb
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class ColorImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "rgb color export"() {
        given:
        ColorImpl color = new ColorImpl().setRGB(0.1, 0.2, 0.3)

        when:
        color.export(exporter)

        then:
        String expected = """
                <RGB>0.1,0.2,0.3</RGB>
        """
        assertXmlEqualsWrapRoot(exporter.buildString(), expected)
    }

    void "Conversion differently defined colors works"() {
        given:
        ColorImpl c1 = new ColorImpl().setRGB(10, 20, 30)
        ColorImpl c2 = new ColorImpl().setRGB(0.0392156862745098, 0.0784313725490196, 0.11764705882352941)
        when:
        ColorRgb rgb1 = c1.colorRgb
        ColorRgb rgb2 = c2.colorRgb
        then:
        assert [rgb1.red, rgb1.green, rgb1.blue] == [rgb2.red, rgb2.green, rgb2.blue]
    }

    void "generic fluent api works even inherited method from NodeImpl"() {
        given:
        Color c = new ColorImpl()

        when:
        c.setName("FluentColor").setComment("FluentComment").setRGB(0.4, 0.5, 0.6)

        then:
        c.name == "FluentColor"
        c.comment == "FluentComment"
        0.5d == ((ColorImpl) c).getColorRgb().green
    }

    def "rgb color by awt color"() {
        when:
        ColorImpl c = new ColorImpl().setRGB(new java.awt.Color(10, 20, 30))

        then:
        c.colorRgb.red == 0.0392156862745098d
        c.colorRgb.green == 0.0784313725490196d
        c.colorRgb.blue == 0.11764705882352941d
    }
}