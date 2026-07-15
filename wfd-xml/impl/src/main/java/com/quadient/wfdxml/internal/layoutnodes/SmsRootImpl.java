package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.SmsRoot;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class SmsRootImpl extends NodeImpl<SmsRoot> implements SmsRoot {
    private Flow content;

    @Override
    public SmsRoot setContent(Flow flow) {
        this.content = flow;
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("SMSRoot");
        exporter.beginElement("Id").addStringAttribute("Name", "SMS").addPCData("Def.SMSRoot").endElement();
        if (content != null) {
            exporter.addElementWithIface("FlowId", content);
        } else {
            exporter.addElement("FlowId");
        }
        exporter.endElement();
    }
}
