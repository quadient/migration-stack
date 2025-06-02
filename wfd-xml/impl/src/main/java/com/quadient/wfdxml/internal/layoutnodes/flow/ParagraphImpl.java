package com.quadient.wfdxml.internal.layoutnodes.flow;

import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle;
import com.quadient.wfdxml.api.layoutnodes.flow.Paragraph;
import com.quadient.wfdxml.internal.layoutnodes.FlowImpl;
import com.quadient.wfdxml.internal.layoutnodes.ParagraphStyleImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

public class ParagraphImpl implements Paragraph, XmlExportable {

    private final List<TextImpl> texts = new ArrayList<>();
    private ParagraphStyleImpl paragraphStyle = null;
    private String existingParagraphStyleId = null;
    private FlowImpl parent;

    public ParagraphImpl() {
    }

    public ParagraphImpl(FlowImpl parent) {
        this.parent = parent;
    }

    public ParagraphStyleImpl getParagraphStyle() {
        return paragraphStyle;
    }

    @Override
    public ParagraphImpl setParagraphStyle(ParagraphStyle paragraphStyle) {
        this.paragraphStyle = (ParagraphStyleImpl) paragraphStyle;
        return this;
    }

    @Override
    public Paragraph setExistingParagraphStyle(String id) {
        this.existingParagraphStyleId = id;
        return this;
    }

    @Override
    public TextImpl addText() {
        TextImpl t = new TextImpl(this);
        texts.add(t);
        return t;
    }

    @Override
    public FlowImpl back() {
        if (parent == null) {
            throw new IllegalStateException("Parent is not set");
        }
        return parent;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("P");

        if (paragraphStyle != null) {
            exporter.addIfaceAttribute("Id", paragraphStyle);
        } else if (existingParagraphStyleId != null) {
            exporter.addStringAttribute("Id", existingParagraphStyleId);
        }

        for (TextImpl text : texts) {
            text.export(exporter);
        }

        exporter.endElement();
    }
}