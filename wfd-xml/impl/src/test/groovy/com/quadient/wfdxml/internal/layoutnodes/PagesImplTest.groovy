package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import com.quadient.wfdxml.utils.AssertXml
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.Pages.PageConditionType.SIMPLE
import static com.quadient.wfdxml.api.layoutnodes.Pages.PageOrder
import static com.quadient.wfdxml.api.layoutnodes.Pages.PageOrder.VARIABLE_SELECTION
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageConditionType
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.pageConditionTypeToXml

class PagesImplTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "pageSelectionTypeToXml"() {
        expect:
        new PagesImpl().setPageOrder(pageOrder as PageOrder).pageSelectionTypeToXml() == expectedXmlName

        where:
        expectedXmlName | _
        "Simple"        | _
        "Variable"      | _
        "DataVariable"  | _

        pageOrder << PageOrder.values()
    }

    def "pageConditionTypeToXml"() {
        expect:
        pageConditionTypeToXml(pageConditionType as PageConditionType) == expectedXmlName

        where:
        expectedXmlName | _
        "Simple"        | _
        "Integer"       | _
        "Interval"      | _
        "Condition"     | _
        "String"        | _
        "InlCond"       | _
        "Content"       | _

        pageConditionType << PageConditionType.values()
    }


    def "export empty Pages"() {
        given:
        PagesImpl pages = new PagesImpl()

        when:
        pages.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
            <SelectionType>Simple</SelectionType>
        """)
    }

    def "export Pages with ConditionType SIMPLE and with all values set"() {
        given:
        PageImpl startPage = new PageImpl(null)
        String startPageId = exporter.idRegister.getOrCreateId(startPage)
        Pages pages = new PagesImpl() as Pages
        pages.setPageOrder(VARIABLE_SELECTION)
        pages.setType(SIMPLE)
        pages.setStartPage(startPage)

        when:
        (pages as PagesImpl).export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                <SelectionType>Variable</SelectionType>
                <ConditionType>Simple</ConditionType>
                <FirstPageId>$startPageId</FirstPageId> 
            """)
    }

    def "export Pages with main flow and interactive flows"() {
        given:
        Flow mainFlow = new FlowImpl()
        Flow interactiveFlow1 = new FlowImpl()
        Flow interactiveFlow2 = new FlowImpl()
        PagesImpl pages = new PagesImpl()
                .setMainFlow(mainFlow)
                .setInteractiveFlows([interactiveFlow1, interactiveFlow2])

        when:
        pages.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
            <SelectionType>Simple</SelectionType>
            <MainFlow>SR_1</MainFlow> 
            <UseAnotherFlowAsInteractiveMainFlow>False</UseAnotherFlowAsInteractiveMainFlow>
            <InteractiveFlow>
                <FlowId>SR_2</FlowId>
                <FlowType>Normal</FlowType>
            </InteractiveFlow>
            <InteractiveFlow>
                <FlowId>SR_3</FlowId>
                <FlowType>Normal</FlowType>
            </InteractiveFlow>
        """)
    }
}