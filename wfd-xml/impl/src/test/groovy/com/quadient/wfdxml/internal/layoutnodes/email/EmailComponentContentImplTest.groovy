package com.quadient.wfdxml.internal.layoutnodes.email

import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentContent
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.email.EmailComponentContent.ContentType
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class EmailComponentContentImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty EmailComponentContent"() {
        given:
        EmailComponentContent content = new EmailComponentContentImpl()

        when:
        content.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <Type>Text</Type>
                <ContentId/>
                <AlternateTextVarId/>
                <Padding>
                    <Left>0px</Left>
                    <Top>0px</Top>
                    <Right>0px</Right>
                    <Bottom>0px</Bottom>
                </Padding>
                <FillStyleId/>
                """)
    }

    def "export allSet EmailComponentContent"() {
        given:
        Flow flow = Mock()
        FillStyle fillStyle = Mock()

        EmailComponentContent content = new EmailComponentContentImpl() as EmailComponentContent
        content
                .setType(ContentType.IMAGE)
                .setContent(flow)
                .setPaddingLeft(5)
                .setPaddingTop(6)
                .setPaddingRight(7)
                .setPaddingBottom(8)
                .setFillStyle(fillStyle)

        String idFlow = exporter.idRegister.getOrCreateId(flow)
        String idFillStyle = exporter.idRegister.getOrCreateId(fillStyle)

        when:
        (content as EmailComponentContentImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <Type>Image</Type>
                <ContentId>$idFlow</ContentId>
                <AlternateTextVarId/>
                <Padding>
                    <Left>5px</Left>
                    <Top>6px</Top>
                    <Right>7px</Right>
                    <Bottom>8px</Bottom>
                </Padding>
                <FillStyleId>$idFillStyle</FillStyleId>
                """)
    }

    def "xml element name is ECContent"() {
        expect:
        new EmailComponentContentImpl().getXmlElementName() == "ECContent"
    }

    def "ContentType #type maps to XML #expected"() {
        given:
        EmailComponentContent content = new EmailComponentContentImpl()
        content.setType(type)

        when:
        (content as EmailComponentContentImpl).export(exporter)

        then:
        exporter.buildString().contains("<Type>$expected</Type>")

        where:
        type               | expected
        ContentType.TEXT   | "Text"
        ContentType.IMAGE  | "Image"
    }
}
