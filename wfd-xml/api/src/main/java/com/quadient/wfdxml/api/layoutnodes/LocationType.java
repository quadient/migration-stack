package com.quadient.wfdxml.api.layoutnodes;

public enum LocationType {
    DISK("DiskLocation"),
    ICM("VCSLocation"),
    FONT("FONT_DIR");

    private final String xmlValue;

    LocationType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String getXmlValue() {
        return xmlValue;
    }
}
