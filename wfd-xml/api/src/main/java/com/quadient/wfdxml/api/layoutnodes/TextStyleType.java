package com.quadient.wfdxml.api.layoutnodes;

public enum TextStyleType {
    SIMPLE("Simple"),
    DELTA("Delta");

    private final String xmlValue;

    TextStyleType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String getXmlValue() {
        return xmlValue;
    }
}
