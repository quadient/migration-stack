package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.Flow.Type
import static com.quadient.wfdxml.api.layoutnodes.Flow.Type.DIRECT_EXTERNAL
import static com.quadient.wfdxml.api.layoutnodes.Flow.Type.REPEATED
import static com.quadient.wfdxml.api.layoutnodes.Flow.Type.SELECT_BY_CONDITION
import static com.quadient.wfdxml.api.layoutnodes.Flow.Type.SELECT_BY_INLINE_CONDITION
import static com.quadient.wfdxml.api.layoutnodes.Flow.Type.VARIABLE_FORMATTED
import static com.quadient.wfdxml.api.layoutnodes.Flow.Type.OVERFLOWABLE_VARIABLE_FORMATTED
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class FlowImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "def flow serialization"() {
        given:
        FlowImpl flow = new FlowImpl()

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <Type>Simple</Type>
            <FlowContent Width="0.2"/>
            <SectionFlow>False</SectionFlow>
            """)
    }

    def "flow serialization all values set"() {
        given:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl()
        VariableImpl variable = new VariableImpl()
        Flow flow = new FlowImpl()
                .setSectionFlow(true)
                .setType(REPEATED)
                .setVariable(variable)
        flow.addParagraph().setParagraphStyle(paragraphStyle)
        flow.addParagraph().setParagraphStyle(paragraphStyle)

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <Type>Repeated</Type>
            <FlowContent Width="0.2">
                <P Id="SR_1"></P>
                <P Id="SR_1"></P>
            </FlowContent>
            <Variable>SR_2</Variable>
            <SectionFlow>True</SectionFlow>
            """)
    }

    def "export Flow with VariableFormatted type"() {
        given:
        VariableImpl variable = new VariableImpl()
        FlowImpl flow = new FlowImpl()
                .setType(VARIABLE_FORMATTED)
                .setVariable(variable)

        String idVariable = exporter.idRegister.getOrCreateId(variable)

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
        <Type>VariableFormatted</Type>
        <FlowContent Width="0.2"></FlowContent>
        <Variable>$idVariable</Variable>
        <SectionFlow>False</SectionFlow>
        """)
    }

    def "export Flow with unsupported type"() {
        given:
        FlowImpl flow = new FlowImpl()
                .setType(OVERFLOWABLE_VARIABLE_FORMATTED)

        when:
        flow.export(exporter)

        then:
        thrown(UnsupportedOperationException)
    }

    def "flow serialization with paragraphs and text IntegrationTest"() {
        given:
        FlowImpl flow = new FlowImpl()
        TextStyleImpl textStyle1 = new TextStyleImpl()
        TextStyleImpl textStyle2 = new TextStyleImpl()
        ParagraphStyleImpl paragraph1 = new ParagraphStyleImpl()
        ParagraphStyleImpl paragraph2 = new ParagraphStyleImpl()

        flow.addParagraph().setParagraphStyle(paragraph2)
                .addText()
                .appendText("Im working!")
                .appendText(" Have a nice day")
                .setTextStyle(textStyle1)
        flow.addParagraph()
                .setParagraphStyle(paragraph1)
                .addText()
                .appendText("Im working too!")
                .setTextStyle(textStyle2)

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <Type>Simple</Type>
            <FlowContent Width="0.2">
                <P Id="SR_1">
                    <T Id="SR_2" xml:space="preserve">Im working! Have a nice day </T> 
                </P> 
                <P Id="SR_3">
                    <T Id="SR_4" xml:space="preserve">Im working too! </T> 
                </P>
            </FlowContent>
            <SectionFlow>False</SectionFlow>
            """)
    }


    def "export of conversion of Flow type to Xml name"() {
        expect:
        FlowImpl.Type innerType = new FlowImpl().setType(type).getFlowType()
        FlowImpl.convertTypeToXmlName(innerType) == expectedResult

        where:
        expectedResult                  | _
        "Simple"                        | _
        "Integer"                       | _
        "Interval"                      | _
        "Condition"                     | _
        "RawString"                     | _
        "String"                        | _
        "InlCond"                       | _
        "Repeated"                      | _
        "External"                      | _
        "FirstFitting"                  | _
        "FirstFittingAuto"              | _
        "VariableFormatted"             | _
        "OverflowableVariableFormatted" | _
        "Content"                       | _
        "Language"                      | _
        "DirectExternal"                | _
        "DynamicExternal"               | _

        type << Type.values().toList()
    }


    def "export Flow with SelectByCondition"() {
        given:
        VariableImpl variable = new VariableImpl()
        FlowImpl conditionFlow = new FlowImpl()
        FlowImpl defFlow = new FlowImpl()

        FlowImpl flow = new FlowImpl()
                .setType(SELECT_BY_CONDITION)
                .addLineForSelectByCondition(variable, conditionFlow)
                .setDefaultFlow(defFlow)
                .setDefaultError(true)

        String idVariable = exporter.idRegister.getOrCreateId(variable)
        String idConditionFlow = exporter.idRegister.getOrCreateId(conditionFlow)
        String idDefFlow = exporter.idRegister.getOrCreateId(defFlow)

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                             <Type>Condition</Type>
                             <FlowContent Width="0.2"></FlowContent>
                             <Condition VarId="$idVariable">$idConditionFlow</Condition>
                             <Default>$idDefFlow</Default>
                             <TreatDefaultAsError>True</TreatDefaultAsError>
                             <SectionFlow>False</SectionFlow>""")

    }

    def "export SelectByCondition flow with two conditions"() {
        given:
        VariableImpl variable = new VariableImpl()
        VariableImpl secondVariable = new VariableImpl()
        FlowImpl conditionFlow = new FlowImpl()
        FlowImpl secondConditionFlow = new FlowImpl()
        FlowImpl flow = new FlowImpl().setType(SELECT_BY_CONDITION)
                .addLineForSelectByCondition(variable, conditionFlow)
                .addLineForSelectByCondition(secondVariable, secondConditionFlow)

        String idVariable = exporter.idRegister.getOrCreateId(variable)
        String idSecondVariable = exporter.idRegister.getOrCreateId(secondVariable)
        String idConditionFlow = exporter.idRegister.getOrCreateId(conditionFlow)
        String idSecondConditionFlow = exporter.idRegister.getOrCreateId(secondConditionFlow)

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                             <Type>Condition</Type>
                             <FlowContent Width="0.2"></FlowContent>
                             <Condition VarId="$idVariable">$idConditionFlow</Condition>
                             <Condition VarId="$idSecondVariable">$idSecondConditionFlow</Condition>
                             <Default></Default>
                             <TreatDefaultAsError>False</TreatDefaultAsError>
                             <SectionFlow>False</SectionFlow>""")

    }

    def "export empty SelectByInlineCondition Flow"() {
        given:
        Flow flow = new FlowImpl() as Flow
        flow.setType(SELECT_BY_INLINE_CONDITION)

        when:
        (flow as FlowImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                             <Type>InlCond</Type>
                             <FlowContent Width="0.2"></FlowContent>
                             <Variable></Variable>
                             <Default></Default>
                             <TreatDefaultAsError>False</TreatDefaultAsError>
                             <SectionFlow>False</SectionFlow>
                             """)
    }

    def "export SelectByInlineCondition allSet"() {
        given:
        Flow conditionFlow = new FlowImpl()
        Flow secondConditionFlow = new FlowImpl()

        Flow flow = new FlowImpl() as Flow
        flow.setType(SELECT_BY_INLINE_CONDITION)
                .addLineForSelectByInlineCondition("Test script", conditionFlow)
                .addLineForSelectByInlineCondition("Test script 2", secondConditionFlow)

        String idConditionFlow = exporter.idRegister.getOrCreateId(conditionFlow)
        String idSecondConditionFlow = exporter.idRegister.getOrCreateId(secondConditionFlow)

        when:
        (flow as FlowImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                             <Type>InlCond</Type>
                             <FlowContent Width="0.2"></FlowContent>
                             <Variable></Variable>
                             <Condition Value="Test script">$idConditionFlow</Condition>
                             <Condition Value="Test script 2">$idSecondConditionFlow</Condition>
                             <Default></Default>
                             <TreatDefaultAsError>False</TreatDefaultAsError>
                             <SectionFlow>False</SectionFlow>""")
    }

    def "export direct external flow fails when location is not set"() {
        given:
        Flow flow = new FlowImpl()
        flow.setType(DIRECT_EXTERNAL)

        when:
        flow.export(exporter)

        then:
        def exception = thrown(UnsupportedOperationException)
        exception.message == "DirectExternal Flow requires a location"
    }

    def "export direct external flow with location"() {
        given:
        Flow flow = new FlowImpl()
        flow.setType(DIRECT_EXTERNAL).setLocation("icm://somewhere/here.jld")

        when:
        flow.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                             <Type>DirectExternal</Type>
                             <FlowContent Width="0.2"/>
                             <ExternalLocation>icm://somewhere/here.jld</ExternalLocation>
                             <SectionFlow>False</SectionFlow>
                             """)
    }
}