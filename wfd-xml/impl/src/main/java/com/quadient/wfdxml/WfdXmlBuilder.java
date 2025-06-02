package com.quadient.wfdxml;

import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.api.module.TextReplacer;
import com.quadient.wfdxml.api.module.WorkFlowModule;
import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput;
import com.quadient.wfdxml.internal.module.Connection;
import com.quadient.wfdxml.internal.module.WorkFlowModuleImpl;
import com.quadient.wfdxml.internal.module.layout.LayoutImpl;
import com.quadient.wfdxml.internal.module.textreplacer.TextReplacerImpl;
import com.quadient.wfdxml.internal.module.xmldatainput.XmlDataInputImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WfdXmlBuilder {
    private final List<WorkFlowModuleImpl> modules = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private int newModulePositionX = 0;

    public Layout addLayout() {
        LayoutImpl layout = new LayoutImpl();
        setPosition(layout);
        modules.add(layout);
        return layout;
    }

    public TextReplacer addTextReplacer() {
        TextReplacerImpl textReplacer = new TextReplacerImpl();
        setPosition(textReplacer);
        modules.add(textReplacer);
        return textReplacer;
    }

    public XmlDataInput addXmlDataInput() {
        XmlDataInputImpl xmlDataInput = new XmlDataInputImpl();
        setPosition(xmlDataInput);
        modules.add(xmlDataInput);
        return xmlDataInput;
    }

    private void setPosition(WorkFlowModule module) {
        module.setPosX(newModulePositionX);
        newModulePositionX += 15;
    }

    public String build() {
        XmlExporter exporter = new XmlExporter();
        exporter.declaration("1.0", "UTF-8");
        exporter.beginElement("WorkFlow");

        for (WorkFlowModuleImpl module : modules) {
            module.export(exporter);
        }

        for (Connection connect : connections) {
            connect.export(exporter);
        }

        exporter.endElement();
        return exporter.buildString();
    }

    public String buildLayoutDelta() {
        XmlExporter exporter = new XmlExporter();

        Optional<LayoutImpl> layoutModule = modules.stream().filter(LayoutImpl.class::isInstance).map(LayoutImpl.class::cast).findFirst();
        if (layoutModule.isEmpty()) throw new IllegalStateException("No layout module found");

        layoutModule.get().exportLayoutDelta(exporter);

        return exporter.buildString();
    }

    /**
     * @param fromModule first connected module
     * @param fromIndex  index of first module's outcoming port
     * @param toModule   second connected module
     * @param toIndex    index of second module's incoming port
     */
    public void connectModules(WorkFlowModule fromModule, int fromIndex, WorkFlowModule toModule, int toIndex) {
        Connection connect = new Connection((WorkFlowModuleImpl) fromModule, fromIndex, (WorkFlowModuleImpl) toModule, toIndex);
        connections.add(connect);
    }
}
