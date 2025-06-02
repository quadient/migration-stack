package com.quadient.wfdxml.api.layoutnodes;

public enum LocationType {
    DISK("DiskLocation"),
    ICM("VCSLocation");

    private final String xmlValue;

    LocationType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String getXmlValue() {
        return xmlValue;
    }
}
