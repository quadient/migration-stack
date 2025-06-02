package com.quadient.wfdxml.internal.layoutnodes.data

import com.quadient.wfdxml.internal.NodeImpl
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.JOB_NAME
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGE_COUNTER
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PREVIOUS_PAGE_INDEX
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PRINTING_FACE
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.CONSTANT
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.DATA_VARIABLE
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.ARRAY
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.OPTIONAL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DOUBLE
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT64
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class DataImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "serialization of DataNode"() {
        given:
        DataImpl data = new DataImpl()
        exporter.getIdRegister().setObjectId(data, "Def.Data")

        when:
        data.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                """)
    }

    def "systemVariableArray is created correctly"() {
        when:
        DataImpl data = new DataImpl()

        then:
        def sysVar = data.systemVariableArray
        sysVar.name == "SystemVariable"
        sysVar.type == DATA_VARIABLE
        sysVar.insideFnc == -10
        sysVar.nodeOptionality == MUST_EXIST
        sysVar.nodeType == SUB_TREE

        data.children.contains(sysVar)

    }

    def "getSystemVariable returns right SystemVariable"() {
        when:
        VariableImpl variable = new DataImpl().getSystemVariable(PAGE_COUNTER) as VariableImpl

        then:
        variable.getName() == "PageCounter"
        variable.getNodeType() == INT
        variable.insideFnc == -11
    }

    def "getSystemVariable returns right SystemVariable mu "() {
        given:
        DataImpl data = new DataImpl()

        when:
        VariableImpl variable1 = data.getSystemVariable(PREVIOUS_PAGE_INDEX) as VariableImpl
        VariableImpl variable2 = data.getSystemVariable(JOB_NAME) as VariableImpl
        VariableImpl variable3 = data.getSystemVariable(PRINTING_FACE) as VariableImpl

        then:
        variable1.getName() == "PreviousPageIndex"
        variable2.getName() == "JobName"
        variable3.getName() == "PrintingFace"

    }

    def "add simple constant variable"() {
        given:
        DataImpl data = new DataImpl()

        when:
        data.addVariable().setValue("TestValue").setName("TestName")

        then:
        List<NodeImpl> expected = data.children.findAll {
            VariableImpl var = it as VariableImpl
            var.name == "TestName" &&
                    var.constantText == "TestValue" &&
                    var.nodeType == STRING &&
                    var.nodeOptionality == MUST_EXIST &&
                    var.type == CONSTANT
        }
        expected.size() == 1

    }


    def "find simple constant variable"() {
        given:
        DataImpl data = new DataImpl()
        data.addVariable().setValue("TestValue").setName("TestName")

        when:
        VariableImpl var = data.findVariable("TestName")

        then:
        var.name == "TestName"
        var.constantText == "TestValue"
        var.nodeType == STRING
        var.nodeOptionality == MUST_EXIST
        var.type == CONSTANT
    }

    def "findVariable throw exception when no variable was found"() {
        given:
        DataImpl data = new DataImpl()

        when:
        data.findVariable("NonExisting")

        then:
        IllegalArgumentException ex = thrown()
        ex.message == "Variable not found. For whole path = [NonExisting]. Part of path, that was successfully found []. Variable which was not found = 'NonExisting'"
    }

    def "findVariable in deeper array structure"() {
        given:
        DataImpl dataImpl = new DataImpl()
        WorkFlowTreeDefinition root =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)
                        .addSubNode(
                                new WorkFlowTreeDefinition("Numbers", SUB_TREE, ARRAY)
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number1", INT64, MUST_EXIST)
                                        )
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number2", INT, MUST_EXIST)
                                        )
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number3", INT, MUST_EXIST)
                                        )
                        )
        dataImpl.importDataDefinition(root)

        when:
        VariableImpl var = dataImpl.findVariable("Numbers", "Number2")


        then:
        var.name == "Number2"
        var.nodeType == INT
        var.nodeOptionality == MUST_EXIST
        var.type == DATA_VARIABLE
    }

    def "findVariable in deeper optional structure"() {
        given:
        DataImpl dataImpl = new DataImpl()
        WorkFlowTreeDefinition root =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)
                        .addSubNode(
                                new WorkFlowTreeDefinition("Numbers", SUB_TREE, OPTIONAL)
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number1", INT64, MUST_EXIST)
                                        )
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number2", INT, MUST_EXIST)
                                        )
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number3", DOUBLE, MUST_EXIST)
                                        )
                        )
        dataImpl.importDataDefinition(root)

        when:
        VariableImpl var = dataImpl.findVariable("Numbers", "Number3")

        then:
        var.name == "Number3"
        var.nodeType == DOUBLE
        var.nodeOptionality == MUST_EXIST
        var.type == DATA_VARIABLE
    }


    def "findVariable in normal subtree"() {
        given:
        DataImpl dataImpl = new DataImpl()
        WorkFlowTreeDefinition root =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)
                        .addSubNode(
                                new WorkFlowTreeDefinition("Numbers", SUB_TREE, MUST_EXIST)
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number1", STRING, MUST_EXIST)
                                        )
                        )
        dataImpl.importDataDefinition(root)

        when:
        VariableImpl var = dataImpl.findVariable("Numbers", "Number1")

        then:
        var.name == "Number1"
        var.nodeType == STRING
        var.nodeOptionality == MUST_EXIST
        var.type == DATA_VARIABLE

    }
}