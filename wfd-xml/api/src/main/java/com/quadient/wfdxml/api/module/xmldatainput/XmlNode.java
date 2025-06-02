package com.quadient.wfdxml.api.module.xmldatainput;

import com.quadient.wfdxml.api.data.converters.FieldConverter;

public interface XmlNode {

    XmlNode addSubNode();

    XmlNode addSubNode(String name, XmlNodeType type, XmlNodeOptionality optionality, XmlNodeChange change, String xmlName);

    XmlNode addSubNode(String name, XmlNodeType type, XmlNodeOptionality optionality, XmlNodeChange change, String xmlName, FieldConverter fieldConvertor);

    XmlNode setType(XmlNodeType type);

    XmlNode setChange(XmlNodeChange change);

    XmlNode setOptionality(XmlNodeOptionality optionality);

    XmlNode setXmlName(String xmlName);

    XmlNode setName(String name);

    /**
     * Field converter can be created by methods in {@link com.quadient.wfdxml.api.data.converters.FieldConverter}
     */
    XmlNode setFieldConverter(FieldConverter fieldConverter);

    XmlNode getParent();
}
