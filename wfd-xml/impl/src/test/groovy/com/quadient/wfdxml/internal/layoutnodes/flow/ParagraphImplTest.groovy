package com.quadient.wfdxml.internal.layoutnodes.flow

import com.quadient.wfdxml.internal.layoutnodes.ParagraphStyleImpl
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEquals
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class ParagraphImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty Paragraph"() {
        given:
        ParagraphImpl p = new ParagraphImpl()

        when:
        p.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(),
                '''<P/>''')
    }

    def "export Paragraph with ParagraphStyle"() {
        given:
        ParagraphImpl p = new ParagraphImpl()

        when:
        p.setParagraphStyle(new ParagraphStyleImpl())
        p.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(),
                '''<P Id="SR_1" />''')
    }

    def "export Paragraph with two text"() {
        given:
        ParagraphImpl paragraph = new ParagraphImpl()
        paragraph.addText().appendText("First text")
        paragraph.addText().appendText("Second text").appendText(" Appended text")

        when:
        paragraph.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <P>
                   <T xml:space="preserve">First text</T>
                   <T xml:space="preserve">Second text Appended text</T>
                </P>""")
    }

    def "export Paragraph with two different texStyle"() {
        given:
        ParagraphImpl paragraph = new ParagraphImpl()
        TextStyleImpl textStyle1 = new TextStyleImpl()
        TextStyleImpl textStyle2 = new TextStyleImpl()

        paragraph.addText().setTextStyle(textStyle1)
        paragraph.addText().setTextStyle(textStyle2)

        when:
        paragraph.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <P>
                   <T Id="SR_1" xml:space="preserve"/>
                   <T Id="SR_2" xml:space="preserve"/>
                </P>""")
    }

    def "export Paragraph with ExistingParagraphStyle"() {
        given:
        ParagraphImpl paragraph = new ParagraphImpl()

        paragraph.setExistingParagraphStyle("ParagraphStyles.Existing Para Style")

        when:
        paragraph.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """<P Id="ParagraphStyles.Existing Para Style"></P>""")
    }

    def "exporting Paragraph with both ParagraphStyle and ExistingParagraphStyle prefers ParagraphStyle"() {
        given:
        ParagraphImpl paragraph = new ParagraphImpl()

        paragraph.setParagraphStyle(new ParagraphStyleImpl())
        paragraph.setExistingParagraphStyle("ParagraphStyles.Existing Para Style")

        when:
        paragraph.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """<P Id="SR_1"></P>""")
    }
}