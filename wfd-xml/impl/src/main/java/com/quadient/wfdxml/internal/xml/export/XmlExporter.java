package com.quadient.wfdxml.internal.xml.export;

import com.quadient.wfdxml.api.Node;

public class XmlExporter {
    private final XmlBuilder builder = new XmlBuilder();
    private IdRegister idRegister = new IdRegister();

    public XmlExporter() {
    }

    public XmlExporter(IdRegister idRegister) {
        this.idRegister = idRegister;
    }

    public IdRegister getIdRegister() {
        return idRegister;
    }

    /**
     * add Xml Declaration e.g. &lt;?xml version="1.0" encoding="UTF-8"&gt; ?
     */
    public XmlExporter declaration(String version, String encoding) {
        builder.declaration(version, encoding);
        return this;
    }

    /**
     * Do not rename - this is compatible with Designer
     */
    public XmlExporter addElementWithStringData(String elementName, String value) {
        if (value != null) {
            builder.e(elementName);
            if (!value.isEmpty()) {
                if (value.charAt(0) == ' ' || value.charAt(value.length() - 1) == ' ') {
                    addStringAttribute("xml:space", "preserve");
                }
                builder.text(value);
            }
            builder.up();
        }
        return this;
    }

    public XmlExporter addElementWithDoubleData(String elementName, double value) {
        builder.e(elementName).text(doubleToString(value)).up();
        return this;
    }

    public XmlExporter addElementWithBoolData(String elementName, boolean value) {
        builder.e(elementName).text(booleanToString(value)).up();
        return this;
    }

    public XmlExporter addElementWithIntData(String elementName, int value) {
        builder.e(elementName).text(intToString(value)).up();
        return this;
    }

    public XmlExporter addElementWithInt64Data(String elementName, long value) {
        builder.e(elementName).text(longToString(value)).up();
        return this;
    }

    /**
     * Do not rename - this is compatible with Designer
     */
    public XmlExporter addElementWithIface(String elementName, Node obj) {
        builder.e(elementName);
        if (obj != null) {
            String id = obj.getId() == null ? objectToId(obj) : obj.getId();
            builder.text(id);
        }
        builder.up();
        return this;
    }

    /**
     * add empty element e.g.  &lt;Forward&gt;
     */
    public XmlExporter addElement(String elementName) {
        builder.e(elementName).up();
        return this;
    }

    public XmlExporter addStringAttribute(String attributeName, String value) {
        builder.a(attributeName, value);
        return this;
    }

    public XmlExporter addDoubleAttribute(String attributeName, double value) {
        builder.a(attributeName, doubleToString(value));
        return this;
    }

    public XmlExporter addBoolAttribute(String attributeName, boolean value) {
        builder.a(attributeName, booleanToString(value));
        return this;
    }

    public XmlExporter addIntAttribute(String attributeName, int value) {
        builder.a(attributeName, intToString(value));
        return this;
    }

    public XmlExporter addInt64Attribute(String attributeName, long value) {
        builder.a(attributeName, longToString(value));
        return this;
    }

    /**
     * If value is null, then attribute is not added
     */
    public XmlExporter addIfaceAttribute(String attributeName, Object value) {
        if (value != null) {
            builder.a(attributeName, objectToId(value));
        }
        return this;
    }

    public XmlExporter addPCData(String text) {
        builder.text(text);
        return this;
    }

    public XmlExporter addPCData(String text, boolean preserveSpaces) {
        addPreserveSpacesAttribute(preserveSpaces);
        builder.text(text);
        return this;
    }

    public XmlExporter addPCData(double text) {
        builder.text(doubleToString(text));
        return this;
    }

    /**
     * Insert just <b>Id</b> of object
     * Do not rename - this is compatible with Designer
     */
    public XmlExporter addPCData(Object obj) {
        builder.text(objectToId(obj));
        return this;
    }

    /**
     * Do not rename - this is compatible with Designer
     */
    public XmlExporter beginElement(String elementName) {
        builder.e(elementName);
        return this;
    }

    /**
     * Do not rename - this is compatible with Designer
     */
    public XmlExporter beginElement(String elementName, boolean preserveSpaces) {
        beginElement(elementName);
        addPreserveSpacesAttribute(preserveSpaces);
        return this;
    }

    /**
     * Do not rename - this is compatible with Designer
     */
    public XmlExporter endElement() {
        builder.up();
        return this;
    }

    private void addPreserveSpacesAttribute(boolean preserveSpaces) {
        if (preserveSpaces) {
            builder.a("xml:space", "preserve");
        }
    }

    /**
     * Insert arbitrary part of xml into result.
     * BE AWARE! because not special characters are escaped, its just inserted.
     */
    public XmlExporter addRawXml(String xmlFragment) {
        builder.rawXml(xmlFragment);
        return this;
    }

    private String doubleToString(double val) {
        return String.valueOf(val);
    }

    private String booleanToString(boolean val) {
        return val ? "True" : "False";
    }

    private String intToString(int val) {
        return String.valueOf(val);
    }

    private String longToString(long val) {
        return String.valueOf(val);
    }

    private String objectToId(Object val) {
        return idRegister.getOrCreateId(val);
    }

    public String buildString() {
        return builder.asString();
    }
}