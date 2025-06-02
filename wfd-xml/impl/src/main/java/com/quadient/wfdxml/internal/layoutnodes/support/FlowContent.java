package com.quadient.wfdxml.internal.layoutnodes.support;

import com.quadient.wfdxml.internal.layoutnodes.flow.ParagraphImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

public class FlowContent implements XmlExportable {
    private final List<ParagraphImpl> paragraphs = new ArrayList<>();

    public ParagraphImpl addParagraph(ParagraphImpl paragraph) {
        paragraphs.add(paragraph);
        return paragraph;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("FlowContent")
                .addDoubleAttribute("Width", 0.2);

        for (ParagraphImpl paragraph : paragraphs) {
            paragraph.export(exporter);
        }
        exporter.endElement();
    }
}