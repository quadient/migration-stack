package com.quadient.wfdxml.internal.module.layout;

import com.quadient.wfdxml.api.layoutnodes.Color;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.DefaultNodeType;
import com.quadient.wfdxml.internal.HasLocalNodes;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ForwardReferencesExporter {

    private final LayoutImpl layout;
    private final Map<DefaultNodeType, DefNode> defNodes;
    private final XmlExporter exporter;

    private Set<NodeImpl> rootDefNodes;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public void exportForwardReferences(Boolean useExisting) {
        exportTree(layout, useExisting);
    }

    private void exportTree(Tree tree, Boolean useExisting) {
        for (Object c : tree.children) {
            NodeImpl child = (NodeImpl) c;
            if (!rootDefNodes.contains(child) && child.getId() == null) {
                writeForwardReferenceToExporter(child, tree, useExisting);
            }
            if (child instanceof Tree) {
                exportTree((Tree) child, useExisting);
            }
            if (child instanceof HasLocalNodes holder && !holder.getLocalNodes().isEmpty()) {
                for (NodeImpl localNode : holder.getLocalNodes()) {
                    writeForwardReferenceToExporter(localNode, child, useExisting);
                }
            }
        }
    }

    private void writeForwardReferenceToExporter(NodeImpl node, NodeImpl parent, Boolean useExisting) {
        exporter.beginElement(node.getXmlElementName()).addElementWithIface("Id", node).addElementWithStringData("Name", node.getName()).addElementWithStringData("Comment", node.getComment());

        if (node instanceof Variable variable && variable.getExistingParentId() != null) {
            exporter.addElementWithStringData("ParentId", variable.getExistingParentId());
        } else {
            exporter.addElementWithIface("ParentId", parent);
        }

        if (node.getDisplayName() != null && !node.getDisplayName().isBlank()) {
            node.addCustomProperty("DisplayName", node.getDisplayName());
        }

        var customProperties = node.getCustomProperties();
        if (customProperties != null && !customProperties.isEmpty()) {
            try {
                String customPropsJson = objectMapper.writeValueAsString(customProperties);
                exporter.addElementWithStringData("CustomProperty", customPropsJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize customProperties to JSON", e);
            }
        }

        var forwardElement = exporter.beginElement("Forward");
        if ((node instanceof Variable || node instanceof Color || node instanceof FillStyle) && useExisting) {
            forwardElement.addBoolAttribute("useExisting", true);
        }
        forwardElement.endElement();

        exporter.endElement();
    }
}