package com.quadient.integration

import com.quadient.examples.ImageAndLineUsageExample
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class ImageAndLineUsageExampleTest extends Specification {

    def "Integration test of Image and line usage example"() {
        when:
        String xmlWorkflow = new ImageAndLineUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/ImageAndLineUsageExample.xml', xmlWorkflow)
    }
}