package com.quadient.integration

import com.quadient.examples.ParagraphUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class ParagraphUsageExampleTest extends Specification {

    def "Integration test of Paragraph usage example"() {
        when:
        String xmlWorkflow = new ParagraphUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/ParagraphUsageExample.xml', xmlWorkflow)
    }
}