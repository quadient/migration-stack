package com.quadient.wfdxml.internal.module;

import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class Connection implements XmlExportable {

    private final WorkFlowModuleImpl fromModule;
    private final WorkFlowModuleImpl todModule;

    private final int fromIndex;
    private final int toIndex;

    public Connection(WorkFlowModuleImpl firstModule, int fromIndex, WorkFlowModuleImpl secondModule, int toIndex) {
        this.fromModule = firstModule;
        this.todModule = secondModule;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("Connect")
                .addElementWithIface("From", fromModule)
                .addElementWithInt64Data("FromIndex", fromIndex)
                .addElementWithIface("To", todModule)
                .addElementWithInt64Data("ToIndex", toIndex)
                .endElement();
    }
}