package com.quadient.wfdxml.internal.layoutnodes.data

import com.quadient.wfdxml.api.layoutnodes.FlowArea
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.Displacement
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.internal.layoutnodes.FlowAreaImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import com.quadient.wfdxml.utils.AssertXml
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.data.DataType.BOOL
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.DATE_TIME
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.INT
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.INT64
import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType
import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.ARABIC_TO_ROMAN_NUMBER
import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.SCRIPT
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.ARRAY_VALUE
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.CALCULATED
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.DISCONNECTED
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.EXTERNAL_CALCULATED
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.GLOBAL_VARIABLE
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.NUMBERING
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.OVERFLOW_MESSAGE
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.CONSTANT
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.DATA_VARIABLE
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.ARRAY
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.OPTIONAL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE

class VariableImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty variable"() {
        given:
        VariableImpl emptyVariable = new VariableImpl()

        when:
        emptyVariable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>DataVariable</Type>
                        <VarType>String</VarType>
                        <InsideFnc>0</InsideFnc>
                        """)
    }

    def "export variable with all values"() {

        given:
        VariableImpl variable = new VariableImpl()
                .setInsideFnc(-11)
                .setType(DATA_VARIABLE)
                .setNodeType(SUB_TREE)
                .setNodeOptionality(ARRAY)

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>DataVariable</Type>
                        <VarType>Array</VarType>
                        <InsideFnc>-11</InsideFnc>
                        """)
    }

    def "InsideFnc is not exported for non DataVariable type"() {
        given:
        VariableImpl variable = new VariableImpl()
                .setInsideFnc(-11)
                .setType(CONSTANT)
                .setNodeType(STRING)
                .setNodeOptionality(MUST_EXIST)

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Constant</Type>
                        <VarType>String</VarType>
                        """)

    }

    def "export constant with value"() {
        given:
        VariableImpl variable = new VariableImpl()
                .setType(CONSTANT)
                .setNodeType(STRING)
                .setValue("MyConstTestValue")

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Constant</Type>
                        <VarType>String</VarType>
                        <Content>MyConstTestValue</Content>
                        """)
    }

    def "export number Constant variable"() {
        given:
        VariableImpl variable = new VariableImpl()
                .setDataType(INT)
                .setKind(VariableKind.CONSTANT)
                .setValue(2)
        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Constant</Type>
                        <VarType>Int</VarType>
                        <Content>2</Content>""")
    }

    def "convert VariableType to xmlName"() {
        when:
        List<String> xmlNames = VariableType.values().collect {
            VariableImpl vi = new VariableImpl()
            vi.convertVariableTypeToXmlName(it)
        }

        then:
        assert xmlNames == [
                "Constant",
                "DataVariable",
                "RealVariable",
                "Calculated",
                "Numbering",
                "Disconnected",
                "OverflowObject",
                "ExternalCalculated",
                "SearchArrayForValue",
        ]
    }

    def "convert NodeType to xmlName"() {
        when:
        List<String> xmlNames = NodeType.values().collect {
            VariableImpl vi = new VariableImpl()
            vi.convertNodeTypeToXmlName(it, MUST_EXIST)
        }

        then:
        assert xmlNames == [
                "String",
                "Int",
                "Int64",
                "Currency",
                "Double",
                "DateTime",
                "Bool",
                "SubTree",
                "Binary",
        ]
    }

    def "convert NodeType special subtree cases"() {
        when:
        List<String> xmlNames = NodeOptionality.values().collect {
            VariableImpl vi = new VariableImpl()
            vi.convertNodeTypeToXmlName(SUB_TREE, it)
        }

        then:
        assert xmlNames == [
                "Optional",
                "SubTree",
                "Array",
        ]
    }


    def "setting nodeOptimality with DataTypes"() {
        when:
        List<NodeOptionality> xmlNames = DataType.values().collect {
            VariableImpl vi = new VariableImpl().setDataType(it) as VariableImpl
            vi.getNodeOptionality()
        }

        then:
        assert xmlNames == [
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                MUST_EXIST,
                OPTIONAL,
                ARRAY
        ]
    }

    def "convert variableKind to xmlName"() {
        expect:
        VariableType innerVariableType = new VariableImpl().setKind(apiVariableKind as VariableKind).getType()
        VariableImpl.convertVariableTypeToXmlName(innerVariableType) == resultXmlName

        where:
        resultXmlName         | _
        "DataVariable"        | _
        "Constant"            | _
        "Calculated"          | _
        "RealVariable"        | _
        "Numbering"           | _
        "Disconnected"        | _
        "OverflowObject"      | _
        "ExternalCalculated"  | _
        "SearchArrayForValue" | _

        apiVariableKind << VariableKind.values()
    }

    def "convert nodeType to xml name"() {
        expect:
        VariableImpl variable = new VariableImpl().setDataType(nodeType as DataType)
        NodeType innerVariableNodeType = variable.getNodeType()
        NodeOptionality innerNodeOptionality = variable.getNodeOptionality()
        VariableImpl.convertNodeTypeToXmlName(innerVariableNodeType, innerNodeOptionality) == expected

        where:
        expected   | _
        "String"   | _
        "Int"      | _
        "Int64"    | _
        "Currency" | _
        "Double"   | _
        "DateTime" | _
        "Bool"     | _
        "Binary"   | _
        "SubTree"  | _
        "Optional" | _
        "Array"    | _

        nodeType << DataType.values()
    }


    def "export of numbering variable"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(NUMBERING).setDataType(INT) as VariableImpl

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Numbering</Type>
                        <VarType>Int</VarType>
                        <Content>0</Content>
                        """)
    }

    def "export of Calculated variable"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(CALCULATED).setDataType(INT).setValue(12).setScript("Script")

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Calculated</Type>
                        <VarType>Int</VarType>
                        <Content>12</Content>
                        <Script>Script</Script>
                        """)
    }

    def "export of Global variable"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(GLOBAL_VARIABLE).setDataType(BOOL).setValue(true)

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>RealVariable</Type>
                        <VarType>Bool</VarType>
                        <Content>True</Content>
                        """)
    }

    def "export of Disconnected Variable"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(DISCONNECTED).setDataType(INT64).setValue(11156L)

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Disconnected</Type>
                        <VarType>Int64</VarType>
                        <Content>11156</Content>
                        """)

    }

    def "export of OverFlowMessage Variable"() {
        given:
        FlowArea flowArea = new FlowAreaImpl()
        VariableImpl variable = new VariableImpl().setKind(OVERFLOW_MESSAGE).setFlowArea(flowArea)

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>OverflowObject</Type>
                        <VarType>String</VarType>
                        <OverflowFlowArea>SR_1</OverflowFlowArea>
        """)
    }


    def "export of ExternalCalculated Variable"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(EXTERNAL_CALCULATED).setValue("C://tmp2/wfd-xml")

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                       <Type>ExternalCalculated</Type>
                       <VarType>String</VarType>
                       <Content>C://tmp2/wfd-xml</Content> 
                       <ExternalScript>C://tmp2/wfd-xml</ExternalScript>
        """)
    }


    def "export of ArrayValue Variable"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(ARRAY_VALUE)

        when:
        variable.export(exporter)

        then:
        UnsupportedOperationException exception = thrown()
        exception.message == "This type is not yet supported. type 'SEARCH_ARRAY_FOR_VALUE'"

    }


    def "export of convert ConversionType to Xml name"() {
        expect:
        VariableImpl.convertConversionTypeToXmlName(conversionType) == expectedResult

        where:
        expectedResult         | _
        "ScriptFCV"            | _
        "LatinToRomeNumberFCV" | _

        conversionType << ConversionType.values().toList()
    }

    def "export Variable with roman number"() {
        given:
        VariableImpl variable = new VariableImpl().setDataType(INT).setKind(NUMBERING).setConversion(ARABIC_TO_ROMAN_NUMBER) as VariableImpl

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                       <Type>Numbering</Type>
                       <VarType>Int</VarType>
                       <Content>0</Content> 
                       <FCVClassName>LatinToRomeNumberFCV</FCVClassName>
        """)
    }


    def "export Variable with script"() {
        given:
        String script = "Script"
        VariableImpl variable = new VariableImpl()
                .setDataType(INT).setKind(NUMBERING)
                .setConversion(SCRIPT)
                .setScript(script) as VariableImpl

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                       <Type>Numbering</Type>
                       <VarType>Int</VarType>
                       <Content>0</Content> 
                       <FCVClassName>ScriptFCV</FCVClassName>
                       <FCVProps>
                           <InputType>Int</InputType>
                           <OutputType>String</OutputType>
                           <Script>Script</Script>
                       </FCVProps>
        """)
    }

    def "export Variable with displacement"() {
        given:
        VariableImpl variable = new VariableImpl().setDisplacement(Displacement.REQUIRED) as VariableImpl

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>DataVariable</Type>
                        <VarType>String</VarType>
                        <InsideFnc>0</InsideFnc>
                        <Displacement>Required</Displacement>
                        """)
    }

    def "export datetime disconnected variable with default value"() {
        given:
        VariableImpl variable = new VariableImpl().setKind(DISCONNECTED).setDataType(DATE_TIME).setValue("2014-07-15") as VariableImpl

        when:
        variable.export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <Type>Disconnected</Type>
                        <VarType>DateTime</VarType>
                        <Content>2014-07-15</Content>
                        """)
    }
}