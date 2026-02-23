package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.SheetNameType
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
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

    def "add sheet names to Pages"() {
        given:
        PagesImpl pages = new PagesImpl()
        VariableImpl var1 = new VariableImpl()
        VariableImpl var2 = new VariableImpl()

        when:
        pages.addSheetName(SheetNameType.PDF_TITLE, var1)
        pages.addSheetName(SheetNameType.PDF_AUTHOR, var2)

        then:
        pages.getSheetNames().size() == 2
        pages.getSheetNames().get(SheetNameType.PDF_TITLE) == var1
        pages.getSheetNames().get(SheetNameType.PDF_AUTHOR) == var2
    }

    def "export Pages with sheet names - all PDF metadata fields"() {
        given:
        VariableImpl var37 = new VariableImpl()
        VariableImpl var38 = new VariableImpl()
        VariableImpl var39 = new VariableImpl()
        VariableImpl var40 = new VariableImpl()
        VariableImpl var41 = new VariableImpl()
        
        String id37 = exporter.idRegister.getOrCreateId(var37)
        String id38 = exporter.idRegister.getOrCreateId(var38)
        String id39 = exporter.idRegister.getOrCreateId(var39)
        String id40 = exporter.idRegister.getOrCreateId(var40)
        String id41 = exporter.idRegister.getOrCreateId(var41)
        
        PagesImpl pages = new PagesImpl()
                .addSheetName(SheetNameType.PDF_TITLE, var37)
                .addSheetName(SheetNameType.PDF_AUTHOR, var38)
                .addSheetName(SheetNameType.PDF_SUBJECT, var39)
                .addSheetName(SheetNameType.PDF_KEYWORDS, var40)
                .addSheetName(SheetNameType.PDF_PRODUCER, var41)

        when:
        pages.export(exporter)

        then:
        def xml = exporter.buildString()
        (xml =~ /<SheetNameVariableId>[^<]+<\/SheetNameVariableId>/).count == 5
        (xml =~ /<SheetNameVariableId><\/SheetNameVariableId>/).count == 37
        AssertXml.assertXmlEqualsWrapRoot(xml, """
            <SelectionType>Simple</SelectionType>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId></SheetNameVariableId>
            <SheetNameVariableId>$id37</SheetNameVariableId>
            <SheetNameVariableId>$id38</SheetNameVariableId>
            <SheetNameVariableId>$id39</SheetNameVariableId>
            <SheetNameVariableId>$id40</SheetNameVariableId>
            <SheetNameVariableId>$id41</SheetNameVariableId>
        """)
    }

    def "export Pages with sheet names - partial PDF metadata"() {
        given:
        VariableImpl var37 = new VariableImpl()
        VariableImpl var40 = new VariableImpl()
        
        PagesImpl pages = new PagesImpl()
                .addSheetName(SheetNameType.PDF_TITLE, var37)
                .addSheetName(SheetNameType.PDF_KEYWORDS, var40)

        when:
        pages.export(exporter)

        then:
        def xml = exporter.buildString()
        (xml =~ /<SheetNameVariableId>[^<]+<\/SheetNameVariableId>/).count == 2
        (xml =~ /<SheetNameVariableId><\/SheetNameVariableId>/).count == 39
    }

    def "export Pages without sheet names - should not add SheetNameVariableId"() {
        given:
        PagesImpl pages = new PagesImpl()

        when:
        pages.export(exporter)

        then:
        def xml = exporter.buildString()
        !xml.contains("SheetNameVariableId")
    }
}