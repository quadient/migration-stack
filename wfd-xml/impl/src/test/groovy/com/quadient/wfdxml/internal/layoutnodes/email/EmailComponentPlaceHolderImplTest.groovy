package com.quadient.wfdxml.internal.layoutnodes.email

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentPlaceHolder
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.email.EmailComponentPlaceHolder.Type
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class EmailComponentPlaceHolderImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export EmailComponentPlaceHolder without content"() {
        given:
        EmailComponentPlaceHolder placeHolder = new EmailComponentPlaceHolderImpl() as EmailComponentPlaceHolder
        placeHolder.setType(Type.BODY)

        when:
        (placeHolder as EmailComponentPlaceHolderImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <PlaceHolderType>Body</PlaceHolderType>
                <ContentId/>
                """)
    }

    def "export EmailComponentPlaceHolder with content"() {
        given:
        Flow flow = Mock()
        EmailComponentPlaceHolder placeHolder = new EmailComponentPlaceHolderImpl() as EmailComponentPlaceHolder
        placeHolder.setType(Type.HEADER).setContent(flow)

        String idFlow = exporter.idRegister.getOrCreateId(flow)

        when:
        (placeHolder as EmailComponentPlaceHolderImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <PlaceHolderType>Header</PlaceHolderType>
                <ContentId>$idFlow</ContentId>
                """)
    }

    def "xml element name is ECPlaceHolder"() {
        expect:
        new EmailComponentPlaceHolderImpl().getXmlElementName() == "ECPlaceHolder"
    }

    def "Type #type maps to XML #expected"() {
        given:
        EmailComponentPlaceHolder placeHolder = new EmailComponentPlaceHolderImpl() as EmailComponentPlaceHolder
        placeHolder.setType(type)

        when:
        (placeHolder as EmailComponentPlaceHolderImpl).export(exporter)

        then:
        exporter.buildString().contains("<PlaceHolderType>$expected</PlaceHolderType>")

        where:
        type         | expected
        Type.HEADER  | "Header"
        Type.BODY    | "Body"
        Type.FOOTER  | "Footer"
    }
}
