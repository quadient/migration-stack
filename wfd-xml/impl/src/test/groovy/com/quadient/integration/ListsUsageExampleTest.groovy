package com.quadient.integration

import com.quadient.examples.ListsUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class ListsUsageExampleTest extends Specification {

    def "Integration test of List usage rxample"() {
        when:
        String xmlWorkflow = new ListsUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/ListsUsageExample.xml', xmlWorkflow)
    }
}