package com.quadient.wfdxml.internal.module.xmldatainput;


import com.quadient.wfdxml.api.data.converters.FieldConverter;
import com.quadient.wfdxml.api.module.xmldatainput.XmlNode;
import com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange;
import com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality;
import com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType;
import com.quadient.wfdxml.internal.data.converters.DummyFieldConverter;
import com.quadient.wfdxml.internal.data.converters.FieldConverterImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.NONE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ELEMENT;

public class XmlNodeImpl implements XmlNode, XmlExportable {

    private final List<XmlNodeImpl> xmlNodes = new ArrayList<>();
    private String name = "NewNode";
    private XmlNodeType type = ELEMENT;
    private XmlNodeOptionality optionality = ONE;
    private XmlNodeChange change = NONE;
    private String xmlName = "NewNode";
    private XmlNodeImpl parent;
    private FieldConverterImpl fieldConverter = new DummyFieldConverter();

    public String getName() {
        return name;
    }

    @Override
    public XmlNodeImpl setName(String name) {
        this.name = name;
        return this;
    }

    public XmlNodeType getType() {
        return type;
    }

    @Override
    public XmlNodeImpl setType(XmlNodeType type) {
        this.type = type;
        return this;
    }

    public XmlNodeOptionality getOptionality() {
        return optionality;
    }

    @Override
    public XmlNodeImpl setOptionality(XmlNodeOptionality optionality) {
        this.optionality = optionality;
        return this;
    }

    public XmlNodeChange getChange() {
        return change;
    }

    @Override
    public XmlNodeImpl setChange(XmlNodeChange change) {
        this.change = change;
        return this;
    }

    public String getXmlName() {
        return xmlName;
    }

    @Override
    public XmlNodeImpl setXmlName(String xmlName) {
        this.xmlName = xmlName;
        return this;
    }

    @Override
    public XmlNodeImpl addSubNode() {
        XmlNodeImpl xmlNode = new XmlNodeImpl();
        xmlNode.parent = this;
        xmlNodes.add(xmlNode);
        return xmlNode;
    }

    @Override
    public XmlNode addSubNode(String name, XmlNodeType type, XmlNodeOptionality optionality, XmlNodeChange change, String xmlName) {
        return addSubNode(name, type, optionality, change, xmlName, null);
    }

    @Override
    public XmlNode addSubNode(String name, XmlNodeType type, XmlNodeOptionality optionality, XmlNodeChange change, String xmlName, FieldConverter fieldConverter) {
        XmlNodeImpl xmlNode = addSubNode().setName(name).setType(type).setOptionality(optionality).setChange(change).setXmlName(xmlName);
        if (fieldConverter != null) {
            xmlNode.setFieldConverter(fieldConverter);
        }
        return xmlNode;
    }

    public FieldConverterImpl getFieldConverter() {
        return fieldConverter;
    }

    @Override
    public XmlNodeImpl setFieldConverter(FieldConverter fieldConverter) {
        this.fieldConverter = (FieldConverterImpl) fieldConverter;
        return this;
    }

    public List<XmlNodeImpl> getXmlNodes() {
        return xmlNodes;
    }

    @Override
    public XmlNodeImpl getParent() {
        return parent;
    }

    @Override
    public void export(XmlExporter exporter) {
        export(exporter, 1);
    }

    private void export(XmlExporter exporter, int depthLevel) {
        exporter.beginElement("XMLNode")
                .addStringAttribute("Type", convertXmlNodeTypeToXmlName())
                .addIntAttribute("Level", depthLevel)
                .addStringAttribute("Optionality", convertXmlNodeOptionalityToXmlName())
                .addStringAttribute("Change", convertXmlNodeChangeToXmlName())
                .addStringAttribute("Name", name)
                .addStringAttribute("XMLName", xmlName);
        fieldConverter.export(exporter);
        exporter.endElement();
        for (XmlNodeImpl xmlNode : xmlNodes) {
            xmlNode.export(exporter, depthLevel + 1);
        }
    }

    private String convertXmlNodeTypeToXmlName() {
        switch (type) {
            case ELEMENT:
                return "Element";
            case ATTRIBUTE:
                return "Attribute";
            case ONE_OF:
                return "OneOf";
            case SEQUENCE:
                return "Sequence";
            case PC_DATA:
                return "PCData";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    private String convertXmlNodeChangeToXmlName() {
        switch (change) {
            case NONE:
                return "None";
            case IGNORE:
                return "Ignore";
            case FLATTEN:
                return "Flatten";
            case ERROR_RECOVER:
                return "ErrorRecover";
            default:
                throw new IllegalStateException(change.toString());
        }
    }

    private String convertXmlNodeOptionalityToXmlName() {
        switch (optionality) {
            case ONE:
                return "One";
            case ZERO_OR_ONE:
                return "ZeroOrOne";
            case ZERO_OR_MORE:
                return "ZeroOrMore";
            case ONE_OR_MORE:
                return "OneOrMore";
            default:
                throw new IllegalStateException(optionality.toString());
        }
    }
}