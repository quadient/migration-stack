package com.quadient.wfdxml.internal.module.textreplacer;

import com.quadient.wfdxml.api.module.TextReplacer;
import com.quadient.wfdxml.internal.module.WorkFlowModuleImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class TextReplacerImpl extends WorkFlowModuleImpl implements TextReplacer {

    public TextReplacerImpl() {
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("TextReplacer")
                .addElementWithIface("Id", this)
                .addElementWithStringData("Name", getName())
                .beginElement("ModulePos")
                .addIntAttribute("X", getPosX())
                .addIntAttribute("Y", getPosY())
                .endElement()
                .endElement();
    }
}