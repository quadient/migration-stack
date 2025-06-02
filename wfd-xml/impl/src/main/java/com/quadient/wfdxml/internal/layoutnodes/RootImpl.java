package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Root;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class RootImpl implements Root, XmlExportable {

    private boolean allowRuntimeModifications = false;

    @Override
    public Root setAllowRuntimeModifications(boolean allowRuntimeModifications) {
        this.allowRuntimeModifications = allowRuntimeModifications;
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithBoolData("AllowRuntimeModifications", allowRuntimeModifications);
    }
}