package com.quadient.wfdxml.internal.module

import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class ConnectionTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "connect two WorkflowModule"() {
        given:
        WorkFlowModuleImpl mock = Mock()
        WorkFlowModuleImpl mock2 = Mock()

        Connection connection = new Connection(mock, 0, mock2, 0)

        when:
        connection.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <Connect>
                    <From>SR_1</From>
                    <FromIndex>0</FromIndex>
                    <To>SR_2</To>
                    <ToIndex>0</ToIndex>
                </Connect> """)
    }
}