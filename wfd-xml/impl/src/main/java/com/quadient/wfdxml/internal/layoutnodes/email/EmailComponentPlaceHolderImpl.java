package com.quadient.wfdxml.internal.layoutnodes.email;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentPlaceHolder;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class EmailComponentPlaceHolderImpl extends NodeImpl<EmailComponentPlaceHolder> implements EmailComponentPlaceHolder {

    private Type type;
    private Flow content;

    @Override
    public EmailComponentPlaceHolder setContent(Flow flow) {
        this.content = flow;
        return this;
    }

    @Override
    public EmailComponentPlaceHolder setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "ECPlaceHolder";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithStringData("PlaceHolderType", xmlName(type));
        if (content != null) {
            exporter.addElementWithIface("ContentId", content);
        } else {
            exporter.addElement("ContentId");
        }
    }

    private static String xmlName(Type type) {
        switch (type) {
            case HEADER:
                return "Header";
            case BODY:
                return "Body";
            case FOOTER:
                return "Footer";
            default:
                throw new IllegalStateException(type.toString());
        }
    }
}
