package com.quadient.wfdxml.internal.module.layout;

import com.quadient.wfdxml.internal.NodeImpl;

public class DefNode {
    public final NodeImpl node;
    public final String xmlId;

    public DefNode(NodeImpl node, String xmlId) {
        this.node = node;
        this.xmlId = xmlId;
    }
}
