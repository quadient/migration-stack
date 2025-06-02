package com.quadient.integration

import com.quadient.examples.VariableUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class VariableUsageExampleTest extends Specification {

    def "Integration test of variable usage example"() {
        when:
        String xmlWorkflow = new VariableUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/VariableUsageExample.xml', xmlWorkflow) { nodeBuilder ->
            nodeBuilder.withNodeFilter { node ->
                //ignore "//XMLDataInput/Location" nodes, because there is used system temp dir
                !(node.nodeName == "Location" && node.parentNode.nodeName == "XMLDataInput")
            }
        }
    }
}