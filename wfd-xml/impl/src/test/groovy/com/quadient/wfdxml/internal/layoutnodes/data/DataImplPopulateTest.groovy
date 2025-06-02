package com.quadient.wfdxml.internal.layoutnodes.data

import com.quadient.wfdxml.api.data.DataDefinition
import com.quadient.wfdxml.internal.NodeImpl
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import spock.lang.Specification

import static com.quadient.wfdxml.internal.layoutnodes.data.VariableImplUtil.assertVariable
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.ARRAY
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.OPTIONAL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.BOOL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE

class DataImplPopulateTest extends Specification {

    def "importDataDefinition basic array structure from data generator"() {
        given:
        DataImpl dataImpl = new DataImpl()
        WorkFlowTreeDefinition root =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY).addSubNode(
                        new WorkFlowTreeDefinition("Numbers", SUB_TREE, ARRAY).addSubNode(
                                new WorkFlowTreeDefinition("Number", INT, MUST_EXIST)
                        )
                )

        when:
        dataImpl.importDataDefinition(root)

        then:
        List<NodeImpl> vars = removeSystemVariables(dataImpl.children)
        assert vars.size() == 1
        assertVariable(vars[0], "Numbers", SUB_TREE, ARRAY, 0) { List<VariableImpl> numbersChildren ->
            assert numbersChildren.size() == 2
            assertVariable(numbersChildren[0], "Value", SUB_TREE, MUST_EXIST, -1) { List<VariableImpl> valueChildren ->
                assert valueChildren.size() == 1
                assertVariable(valueChildren[0], "Number", INT, MUST_EXIST, 0)
            }
            assertVariable(numbersChildren[1], "Count", INT, MUST_EXIST, -2)
        }

    }

    private List<NodeImpl> removeSystemVariables(List<NodeImpl> vars) {
        vars = vars.findAll { it.name != "SystemVariable" }
        vars
    }

    def "importDataDefinition cannot be called twice"() {
        given:
        DataImpl dataImpl = new DataImpl()
        DataDefinition definition = new WorkFlowTreeDefinition()
        dataImpl.importDataDefinition(definition)

        when:
        dataImpl.importDataDefinition(definition)

        then:
        IllegalStateException ex = thrown()
        ex.message == "Method 'importDataDefinition' can be called only once."

    }

    def "importDataDefinition import normal subtree with must exist optionality"() {

        DataImpl dataImpl = new DataImpl()
        WorkFlowTreeDefinition root =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY)
                        .addSubNode(
                                new WorkFlowTreeDefinition("Numbers", SUB_TREE, MUST_EXIST)
                                        .addSubNode(
                                                new WorkFlowTreeDefinition("Number1", STRING, MUST_EXIST)
                                        )
                        )

        when:
        dataImpl.importDataDefinition(root)

        then:
        List<NodeImpl> vars = removeSystemVariables(dataImpl.children)
        assert vars.size() == 1
        assertVariable(vars[0], "Numbers", SUB_TREE, MUST_EXIST, 0) { List<VariableImpl> numbersChildren ->
            assert numbersChildren.size() == 1
            assertVariable(numbersChildren[0], "Number1", STRING, MUST_EXIST, 0)
        }

    }

    def "importDataDefinition basic optional structure from data generator"() {
        given:
        DataImpl dataImpl = new DataImpl()
        WorkFlowTreeDefinition root =
                new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY).addSubNode(
                        new WorkFlowTreeDefinition("Node3", SUB_TREE, OPTIONAL)
                                .addSubNode(
                                        new WorkFlowTreeDefinition("Node3Data", STRING, MUST_EXIST)
                                )
                )

        when:
        dataImpl.importDataDefinition(root)

        then:
        List<NodeImpl> vars = removeSystemVariables(dataImpl.children)
        assert vars.size() == 1
        assertVariable(vars[0], "Node3", SUB_TREE, OPTIONAL, 0) { List<VariableImpl> numbersChildren ->
            assert numbersChildren.size() == 2
            assertVariable(numbersChildren[0], "Value", SUB_TREE, MUST_EXIST, -1) { List<VariableImpl> valueChildren ->
                assert valueChildren.size() == 1
                assertVariable(valueChildren[0], "Node3Data", STRING, MUST_EXIST, 0)
            }
            assertVariable(numbersChildren[1], "Exists", BOOL, MUST_EXIST, -2)
        }

    }
}