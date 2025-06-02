package com.quadient.wfdxml.internal.module.xmldatainput

import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.data.converters.BT2BTFCV
import spock.lang.Specification

import static com.quadient.wfdxml.api.data.converters.Converters.boolConverter
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.FLATTEN
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.NONE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE_OR_MORE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ZERO_OR_ONE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ATTRIBUTE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ELEMENT
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.PC_DATA
import static com.quadient.wfdxml.internal.data.WorkFlowTreeDefinitionUtil.assertWorkflowTreeDefinitionsEquals
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.ARRAY
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.OPTIONAL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.BOOL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE

class XmlDataInputImpl_generateWorkflowTreeDefinitionTest extends Specification {

    def "generateWorkflowTreeDefinition empty structure"() {
        given:
        XmlDataInput xmlDataInput = new XmlDataInputImpl()

        when:
        WorkFlowTreeDefinition resultWtd = xmlDataInput.generateDataDefinition()

        then:
        WorkFlowTreeDefinition expectedWtd = new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)

        assertWorkflowTreeDefinitionsEquals(expectedWtd, resultWtd)
    }

    def "generateWorkflowTreeDefinition complex structure"() {
        given:
        XmlDataInput xmlDataInput = new XmlDataInputImpl()
        // @formatter:off
        xmlDataInput
            .addRootXmlNode().setName("Root").setChange(FLATTEN).setXmlName("Root")
                .addSubNode().setName("Attr1").setType(ATTRIBUTE).setOptionality(ONE).setChange(NONE).setXmlName("XmlAttr1")
                .getParent()
                .addSubNode().setName("Node2").setType(ELEMENT).setOptionality(ONE).setChange(NONE).setXmlName("Xml2")
                    .addSubNode().setName("InnerAttr1").setType(ATTRIBUTE).setOptionality(ONE).setChange(NONE).setXmlName("XmlAttr11")
                    .getParent()
                    .addSubNode().setName("Inner1").setType(ELEMENT).setOptionality(ONE_OR_MORE).setChange(FLATTEN).setXmlName("Xml11")
                        .addSubNode().setName("Inner2").setType(ELEMENT).setOptionality(ONE_OR_MORE).setChange(NONE).setXmlName("Xml12")
                            .addSubNode().setName("Inner2Data").setType(PC_DATA).setOptionality(ONE).setChange(NONE).setXmlName("#PCData")
                            .getParent()
                        .getParent()
                    .getParent()
                .getParent()
                .addSubNode().setName("Node3").setType(ELEMENT).setOptionality(ZERO_OR_ONE).setChange(NONE).setXmlName("Xml3")
                    .addSubNode().setName("Node3Data").setType(PC_DATA).setOptionality(ZERO_OR_ONE).setChange(NONE).setXmlName("#PCData")
        // @formatter:on

        when:
        WorkFlowTreeDefinition resultWtd = xmlDataInput.generateDataDefinition()

        then:
        WorkFlowTreeDefinition expectedWtd =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)
                        .addSubNode(
                                new WorkFlowTreeDefinition("Attr1", STRING, MUST_EXIST)
                        )
                        .addSubNode(
                                new WorkFlowTreeDefinition("Node2", SUB_TREE, MUST_EXIST)
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("InnerAttr1", STRING, MUST_EXIST)
                                        )
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Inner2", SUB_TREE, ARRAY)
                                                        .addSubNode(
                                                                new WorkFlowTreeDefinition("Inner2Data", STRING, MUST_EXIST)
                                                        )
                                        )
                        )
                        .addSubNode(
                                new WorkFlowTreeDefinition("Node3", SUB_TREE, OPTIONAL)
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Node3Data", STRING, MUST_EXIST)
                                        )
                        )

        assertWorkflowTreeDefinitionsEquals(expectedWtd, resultWtd)
    }

    def "generateWorkflowTreeDefinition throws exception when input type of converter is not STRING"() {
        given:
        XmlDataInput xmlDataInput = new XmlDataInputImpl()
        // @formatter:off
            xmlDataInput
                .addRootXmlNode().setName("Root").setChange(FLATTEN).setXmlName("Root")
                    .addSubNode().setName("BadInputType").setType(PC_DATA).setFieldConverter(new BT2BTFCV().setInputType(BOOL))
        // @formatter:on

        when:
        xmlDataInput.generateDataDefinition()

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains("STRING")
    }

    def "generateWorkflowTreeDefinition FieldConverter with bool type"() {
        given:
        XmlDataInputImpl xmlDataInput = new XmlDataInputImpl()
        // @formatter:off
            xmlDataInput
                .addRootXmlNode().setName("Root").setChange(FLATTEN).setXmlName("Root")
                    .addSubNode().setName("BoolConvert").setType(PC_DATA).setFieldConverter(boolConverter())
        // @formatter:on

        when:
        WorkFlowTreeDefinition resultWtd = xmlDataInput.generateDataDefinition()

        then:
        WorkFlowTreeDefinition expectedWtd =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)
                        .addSubNode(
                                new WorkFlowTreeDefinition("BoolConvert", BOOL, MUST_EXIST)
                        )

        assertWorkflowTreeDefinitionsEquals(expectedWtd, resultWtd)
    }
}
