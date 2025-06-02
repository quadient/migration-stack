package com.quadient.wfdxml.internal.xml.export;

import com.quadient.wfdxml.exceptions.WfdXmlException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

public class XmlBuilder {

    private static final String XML_FACTORY_ID = "xmlBuilderFactory";
    private final ByteArrayOutputStream byteArrayOutputStream;
    private final XMLStreamWriter xmlStreamWriter;

    public XmlBuilder() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            System.setProperty(XML_FACTORY_ID, System.getProperty(XML_FACTORY_ID, "com.sun.xml.internal.stream.XMLOutputFactoryImpl"));
            XMLOutputFactory factory = XMLOutputFactory.newFactory(XML_FACTORY_ID, this.getClass().getClassLoader());
            factory.setProperty("escapeCharacters", false);
            xmlStreamWriter = factory.createXMLStreamWriter(new OutputStreamWriter(byteArrayOutputStream, UTF_8));
        } catch (XMLStreamException e) {
            throw new WfdXmlException("Unexpected xml builder error", e);
        }
    }

    public XmlBuilder declaration(String version, String encoding) {
        try {
            xmlStreamWriter.writeStartDocument(encoding, version);
        } catch (XMLStreamException e) {
            throw new WfdXmlException(e);
        }
        return this;
    }

    public XmlBuilder e(String elementName) {
        try {
            xmlStreamWriter.writeStartElement(elementName);
        } catch (XMLStreamException e) {
            throw new WfdXmlException(e);
        }
        return this;
    }

    public XmlBuilder up() {
        try {
            xmlStreamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new WfdXmlException(e);
        }
        return this;
    }

    public XmlBuilder a(String attributeName, String value) {
        try {
            xmlStreamWriter.writeAttribute(attributeName, value);
        } catch (XMLStreamException e) {
            throw new WfdXmlException(e);
        }
        return this;
    }

    public XmlBuilder text(String text) {
        if (text == null) return this;
        writeEscapedCharacters(text);
        return this;
    }

    /**
     * Insert arbitrary part of xml into result.
     * BE AWARE! because no special characters are escaped, its just inserted.
     */
    public XmlBuilder rawXml(String xmlFragment) {
        try {
            xmlStreamWriter.writeCharacters("");
            xmlStreamWriter.flush();
            byteArrayOutputStream.write(xmlFragment.getBytes(UTF_8));
            byteArrayOutputStream.flush();
        } catch (XMLStreamException | IOException e) {
            throw new WfdXmlException(e);
        }
        return this;
    }

    private void writeEscapedCharacters(String text) {
        try {
            xmlStreamWriter.writeCharacters(escapeCharacters(text, true));
        } catch (XMLStreamException e) {
            throw new WfdXmlException(e);
        }
    }

    private String escapeCharacters(String string, boolean aSpaceBeginEnd) {
        StringBuilder sb = new StringBuilder();
        char[] text = string.toCharArray();
        for (int i = 0; i < text.length; i++) {
            char c = text[i];
            if (c < 0x20 || (c == 0x20 && aSpaceBeginEnd && (i == 0 || i == text.length - 1))) {
                int ordinal = c;
                sb.append("&#x").append(Integer.toHexString(ordinal)).append(";");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '\'') {
                sb.append("&#39;");
            } else if (c == '\"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String asString() {
        try {
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.close();
            return byteArrayOutputStream.toString();
        } catch (XMLStreamException e) {
            throw new WfdXmlException(e);
        }
    }
}