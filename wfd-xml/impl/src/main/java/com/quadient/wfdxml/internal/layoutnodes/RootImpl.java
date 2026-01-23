package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.Root;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

public class RootImpl implements Root, XmlExportable {

    private boolean allowRuntimeModifications = false;
    private String externalStylesLayoutVcsLocation = null;
    private String subject = null;
    private final List<Node<?>> lockedWebNodes = new ArrayList<>();

    @Override
    public Root setAllowRuntimeModifications(boolean allowRuntimeModifications) {
        this.allowRuntimeModifications = allowRuntimeModifications;
        return this;
    }

    @Override
    public Root setExternalStylesLayout(String vcsLocation) {
        this.externalStylesLayoutVcsLocation = vcsLocation;
        return this;
    }

    @Override
    public Root setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    @Override
    public Root addLockedWebNode(Node<?> node) {
        lockedWebNodes.add(node);
        return this;
    }

    @Override
    public List<Node<?>> getLockedWebNodes() {
        return lockedWebNodes;
    }

    @Override
    public void export(XmlExporter exporter) {
        if (allowRuntimeModifications) {
            exporter.addElementWithBoolData("AllowRuntimeModifications", allowRuntimeModifications);
        }
        if (externalStylesLayoutVcsLocation != null) {
            exporter.addElementWithStringData("ExternalStylesLayout", "VCSLocation," + externalStylesLayoutVcsLocation);
        }
        if (subject != null) {
            exporter.addElementWithStringData("Subject", subject);
        }
        if (!lockedWebNodes.isEmpty()) {
            exporter.beginElement("LockedWebNodes");
            for (Node<?> node : lockedWebNodes) {
                exporter.addElementWithIface("LockedWebNode", node);
            }
            exporter.endElement();
        }
    }
}