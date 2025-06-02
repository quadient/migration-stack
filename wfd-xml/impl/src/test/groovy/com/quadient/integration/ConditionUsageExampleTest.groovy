package com.quadient.integration

import com.quadient.examples.ConditionUsageExample
import spock.lang.Specification
import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class ConditionUsageExampleTest extends Specification {

    def "Integration test of Condition usage example"() {
        when:
        String xmlWorkflow = new ConditionUsageExample().buildXmlWorkflow()

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/ConditionUsageExample.xml', xmlWorkflow)
    }
}