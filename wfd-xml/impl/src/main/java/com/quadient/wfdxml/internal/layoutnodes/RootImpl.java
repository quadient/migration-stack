package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Root;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class RootImpl implements Root, XmlExportable {

    private boolean allowRuntimeModifications = false;
    private String externalStylesLayoutVcsLocation = null;
    private String subject = null;

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
        return null;
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
    }
}