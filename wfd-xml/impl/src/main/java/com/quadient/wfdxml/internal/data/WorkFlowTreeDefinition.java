package com.quadient.wfdxml.internal.data;

import com.quadient.wfdxml.api.data.DataDefinition;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING;

public class WorkFlowTreeDefinition implements DataDefinition {

    private final List<WorkFlowTreeDefinition> subNodes = new ArrayList<>();
    private String caption = "";
    private NodeType nodeType = STRING;
    private NodeOptionality nodeOptionality = MUST_EXIST;

    public WorkFlowTreeDefinition() {
    }

    public WorkFlowTreeDefinition(String caption, NodeType nodeType, NodeOptionality nodeOptionality) {
        this.caption = caption;
        this.nodeType = nodeType;
        this.nodeOptionality = nodeOptionality;
    }

    public String getCaption() {
        return caption;
    }

    public WorkFlowTreeDefinition setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public WorkFlowTreeDefinition setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    public NodeOptionality getNodeOptionality() {
        return nodeOptionality;
    }

    public WorkFlowTreeDefinition setNodeOptionality(NodeOptionality nodeOptionality) {
        this.nodeOptionality = nodeOptionality;
        return this;
    }

    public List<WorkFlowTreeDefinition> getSubNodes() {
        return subNodes;
    }

    public WorkFlowTreeDefinition addSubNode(WorkFlowTreeDefinition subNode) {
        subNodes.add(subNode);
        return this;
    }

    public WorkFlowTreeDefinition addSubNodes(List<WorkFlowTreeDefinition> subNodes) {
        for (WorkFlowTreeDefinition subNode : subNodes) {
            addSubNode(subNode);
        }
        return this;
    }


    @Override
    public String toString() {
        return "WorkFlowTreeDefinition{" +
                "caption='" + caption + '\'' +
                ", nodeType=" + nodeType +
                ", nodeOptionality=" + nodeOptionality +
                ", subNodes=" + subNodes +
                '}';
    }
}
