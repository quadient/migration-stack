package com.quadient.wfdxml.internal.data

class WorkFlowTreeDefinitionUtil {

    static void assertWorkflowTreeDefinitionsEquals(WorkFlowTreeDefinition expected, WorkFlowTreeDefinition result) {
        assert expected.caption == result.caption
        assert expected.nodeType == result.nodeType
        assert expected.nodeOptionality == result.nodeOptionality
        assert expected.subNodes.size() == result.subNodes.size()
        expected.subNodes.eachWithIndex { expectedSubNode, index ->
            def resultSubNode = result.subNodes[index]
            assertWorkflowTreeDefinitionsEquals(expectedSubNode, resultSubNode)
        }
        true
    }
}
