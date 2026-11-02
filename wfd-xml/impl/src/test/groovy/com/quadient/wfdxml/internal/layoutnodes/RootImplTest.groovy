package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class RootImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export Root with external styles layout and data definition"() {
        given:
        RootImpl root = new RootImpl()
        root.setAllowRuntimeModifications(true)
                .setExternalStylesLayout("icm://Interactive/tenant/CompanyStyles/styles.jld")
                .setDataDefinition("icm://Interactive/tenant/DataDefinitions/structure.jdd")

        when:
        root.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <AllowRuntimeModifications>True</AllowRuntimeModifications>
                <ExternalStylesLayout>VCSLocation,icm://Interactive/tenant/CompanyStyles/styles.jld</ExternalStylesLayout>
                <DataDefinition>VCSLocation,icm://Interactive/tenant/DataDefinitions/structure.jdd</DataDefinition>
                """)
    }
}
