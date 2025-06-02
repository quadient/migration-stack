package com.quadient.wfdxml.internal.module.layout;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.DefaultNodeType;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ForwardReferencesExporter {

    private final LayoutImpl layout;
    private final Map<DefaultNodeType, DefNode> defNodes;
    private final XmlExporter exporter;

    private Set<NodeImpl> rootDefNodes;


    public ForwardReferencesExporter(LayoutImpl layout, Map<DefaultNodeType, DefNode> defNodes, XmlExporter exporter) {
        this.layout = layout;
        this.defNodes = defNodes;
        this.exporter = exporter;
        initRootDefNodes(defNodes);
    }

    private void initRootDefNodes(Map<DefaultNodeType, DefNode> defNodes) {
        rootDefNodes = new HashSet<>();
        for (DefNode defNode : defNodes.values()) {
            rootDefNodes.add(defNode.node);
        }
    }

    public void exportForwardReferences() {
        exportTree(layout, false);
    }

    public void exportForwardReferences(Boolean useExistingVariables) {
        exportTree(layout, useExistingVariables);
    }

    private void exportTree(Tree tree, Boolean useExistingVariables) {
        for (Object c : tree.children) {
            NodeImpl child = (NodeImpl) c;
            if (!rootDefNodes.contains(child) && !isWithoutForwardReference(child)) {
                writeForwardReferenceToExporter(child, tree, useExistingVariables);
            }
            if (child instanceof Tree) {
                exportTree((Tree) child, useExistingVariables);
            }
        }
    }

    private final List<String> idsToSkip = List.of("Def.MainFlow");

    private boolean isWithoutForwardReference(NodeImpl node) {
        if (node.getId() == null) {
            return false;
        }

        return idsToSkip.contains(node.getId());
    }

    private void writeForwardReferenceToExporter(NodeImpl node, Tree parent, Boolean useExistingVariables) {
        exporter.beginElement(node.getXmlElementName()).addElementWithIface("Id", node).addElementWithStringData("Name", node.getName()).addElementWithStringData("Comment", node.getComment());

        if (node instanceof Variable variable && variable.getExistingParentId() != null) {
            exporter.addElementWithStringData("ParentId", variable.getExistingParentId());
        } else {
            exporter.addElementWithIface("ParentId", parent);
        }

        var forwardElement = exporter.beginElement("Forward");
        if (node instanceof Variable && useExistingVariables) {
            forwardElement.addBoolAttribute("useExisting", true);
        }
        forwardElement.endElement();

        exporter.endElement();
    }
}