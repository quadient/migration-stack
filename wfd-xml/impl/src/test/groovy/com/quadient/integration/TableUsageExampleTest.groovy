package com.quadient.integration

import com.quadient.examples.TableUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class TableUsageExampleTest extends Specification {

    def "Integration test of table usage example"() {
        when:
        String xmlWorkflow = new TableUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/TableUsageExample.xml', xmlWorkflow)
    }
}