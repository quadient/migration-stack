package com.quadient.wfdxml.internal.layoutnodes.email

import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentRoot
import com.quadient.wfdxml.api.layoutnodes.email.TMText
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class EmailComponentRootImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export default EmailComponentRoot"() {
        given:
        EmailComponentRoot root = new EmailComponentRootImpl()

        when:
        root.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <ECRoot>
                    <Id>Def.EmailDesignRoot</Id>
                    <EmailWidth>600px</EmailWidth>
                </ECRoot>
                """)
    }

    def "export allSet EmailComponentRoot"() {
        given:
        TMText tmText = Mock()
        FillStyle fillStyle = Mock()

        EmailComponentRoot root = new EmailComponentRootImpl() as EmailComponentRoot
        root.setWidth(800)
                .setEmailComponentsText(tmText)
                .setFill(fillStyle)

        String idTmText = exporter.idRegister.getOrCreateId(tmText)
        String idFillStyle = exporter.idRegister.getOrCreateId(fillStyle)

        when:
        (root as EmailComponentRootImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <ECRoot>
                    <Id>Def.EmailDesignRoot</Id>
                    <EmailComponetsText>$idTmText</EmailComponetsText>
                    <EmailWidth>800px</EmailWidth>
                    <FillStyleId>$idFillStyle</FillStyleId>
                </ECRoot>
                """)
    }
}
