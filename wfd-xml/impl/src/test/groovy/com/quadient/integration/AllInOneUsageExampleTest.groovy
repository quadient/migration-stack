package com.quadient.integration

import com.quadient.examples.AllInOneUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class AllInOneUsageExampleTest extends Specification {

    def "Integration test of all in one usage example"() {
        when:
        String xmlWorkflow = new AllInOneUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/AllInOneUsageExample.xml', xmlWorkflow)
    }
}