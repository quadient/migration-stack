package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.LineStyle
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.LineStyle.Endings.CIRCLE
import static com.quadient.wfdxml.api.layoutnodes.LineStyle.Endings.TRIANGLE
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class LineStyleImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty LineStyle"() {
        given:
        LineStyle lineStyle = new LineStyleImpl()

        when:
        lineStyle.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <BeginOffset>0.005</BeginOffset>
                <Stripes>
                    <Stripe Pos="0.0" Width="1.0"/>
                </Stripes>
                <LineEndingStart>-1</LineEndingStart>
                <LineEndingEnd>-1</LineEndingEnd>
                <LineEndingRatio>3.0</LineEndingRatio>""")
    }

    def "export allSet LineStyle"() {
        given:
        LineStyle lineStyle = new LineStyleImpl() as LineStyle
        lineStyle.setBeginOffset(0.01d)
                .addLineGapValues(0.002d)
                .addLineGapValues(0.006d)
                .addLineGapValues(0.007d)
                .addStripe(0.1d, 0.2d)
                .addStripe(-0.6d, 0.6d)
                .setStartEnding(TRIANGLE)
                .setEndEnding(CIRCLE)
                .setSizeEnding(4.8d)

        when:
        (lineStyle as LineStyleImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <BeginOffset>0.01</BeginOffset>
                <DoubleVector>0.002,0.006,0.007</DoubleVector> 
                <Stripes>
                    <Stripe Pos="0.1" Width="0.2"/>
                    <Stripe Pos="-0.6" Width="0.6"/> 
                </Stripes>
                <LineEndingStart>10</LineEndingStart>
                <LineEndingEnd>6</LineEndingEnd>
                <LineEndingRatio>4.8</LineEndingRatio>""")
    }

    def "convert LineStyle Endings to Int"() {
        expect:
        LineStyleImpl.convertEndingsToInt(endings) == a

        where:
        a << (-1..12)
        endings << LineStyle.Endings.values().toList()
    }
}