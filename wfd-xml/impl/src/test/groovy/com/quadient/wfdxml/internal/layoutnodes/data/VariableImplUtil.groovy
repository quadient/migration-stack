package com.quadient.wfdxml.internal.layoutnodes.data

import com.quadient.wfdxml.internal.NodeImpl

class VariableImplUtil {

    static void assertVariable(
            NodeImpl variable,
            String name,
            WorkFlowTreeEnums.NodeType nodeType,
            WorkFlowTreeEnums.NodeOptionality nodeOptionality,
            int insideFnc,
            Closure childrenAssertion = { List<VariableImpl> children ->
                assert children.size() == 0
            }
    ) {
        assert variable instanceof VariableImpl
        VariableImpl var = (VariableImpl) variable
        assert var.name == name
        assert var.nodeType == nodeType
        assert var.nodeOptionality == nodeOptionality
        assert var.insideFnc == insideFnc

        childrenAssertion(var.children)
    }
}