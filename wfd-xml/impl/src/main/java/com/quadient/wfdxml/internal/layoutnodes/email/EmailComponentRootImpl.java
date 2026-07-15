package com.quadient.wfdxml.internal.layoutnodes.email;

import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentRoot;
import com.quadient.wfdxml.api.layoutnodes.email.TMText;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.utils.FormatUtils.formatPx;

public class EmailComponentRootImpl extends NodeImpl<EmailComponentRoot> implements EmailComponentRoot {
    private double width = 600;
    private TMText emailComponentsText;
    private FillStyle fillStyle;

    @Override
    public EmailComponentRoot setWidth(double width) {
        this.width = width;
        return this;
    }

    @Override
    public EmailComponentRoot setEmailComponentsText(TMText flow) {
        this.emailComponentsText = flow;
        return this;
    }

    @Override
    public EmailComponentRoot setFill(FillStyle SFillStyle) {
        this.fillStyle = SFillStyle;
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("ECRoot");
        exporter.addElementWithStringData("Id", "Def.EmailDesignRoot");
        if (emailComponentsText != null) {
            exporter.addElementWithIface("EmailComponetsText", emailComponentsText);
        }
        exporter.addElementWithStringData("EmailWidth", formatPx(width));
        if (fillStyle != null) {
            exporter.addElementWithIface("FillStyleId", fillStyle);
        }
        exporter.endElement();
    }
}
