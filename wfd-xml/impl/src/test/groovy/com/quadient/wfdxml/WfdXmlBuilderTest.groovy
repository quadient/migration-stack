package com.quadient.wfdxml

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.module.Layout
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class WfdXmlBuilderTest extends Specification {

    def "empty workflow have correct xml declaration with version 1 and encoding UTF-8"() {
        given:
        WfdXmlBuilder emptyBuilder = new WfdXmlBuilder()

        when:
        String result = emptyBuilder.build()

        then:
        String expected = '<?xml version="1.0" encoding="UTF-8"?><WorkFlow></WorkFlow>'
        assert result == expected
    }

    def "add new Layout"() {
        given:
        WfdXmlBuilder builder = new WfdXmlBuilder()

        when:
        Layout layout = builder.addLayout()
        layout.addColor().setRGB(0, 220, 0).setName("My Custom Green Color")

        then:
        String xmlWorkflow = builder.build()
        assertXmlFileEquals('com/quadient/wfdxml/workflow/SimpleLayout.xml', xmlWorkflow)
    }

    def "buildLayoutDelta fails when layout module is not added"() {
        given:
        WfdXmlBuilder builder = new WfdXmlBuilder()
        builder.addXmlDataInput()

        when:
        builder.buildLayoutDelta()

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "No layout module found"
    }

    def "buildLayoutDelta creates simplified xml containing only layout module"() {
        given:
        WfdXmlBuilder builder = new WfdXmlBuilder()
        Layout layout = builder.addLayout()
        layout.addFlow().setName("includedFlow").setType(Flow.Type.SIMPLE)

        when:
        def wfdXml = builder.buildLayoutDelta()

        then:
        assertXmlFileEquals("com/quadient/wfdxml/workflow/SimpleDeltaLayout.xml", wfdXml)
    }
}