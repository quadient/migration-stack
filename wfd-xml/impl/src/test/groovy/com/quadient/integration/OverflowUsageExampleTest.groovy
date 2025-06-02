package com.quadient.integration

import com.quadient.examples.OverflowUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class OverflowUsageExampleTest extends Specification {

    def "Integration test of OverFlow usage Example"() {
        when:
        String xmlWorkflow = new OverflowUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/OverflowUsageExample.xml', xmlWorkflow) { nodeBuilder ->
            nodeBuilder.withNodeFilter { node ->
                //ignore "//XMLDataInput/Location" nodes, because there is used system temp dir
                !(node.nodeName == "Location" && node.parentNode.nodeName == "XMLDataInput")
            }
        }
    }
}