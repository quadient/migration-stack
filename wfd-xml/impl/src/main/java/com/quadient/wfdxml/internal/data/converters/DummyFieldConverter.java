package com.quadient.wfdxml.internal.data.converters;

import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class DummyFieldConverter extends FieldConverterImpl<DummyFieldConverter> {

    @Override
    String getFieldConverterClassNameForExport() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    void exportFieldConverterSettings(XmlExporter exporter) {

    }

    @Override
    public void export(XmlExporter exporter) {
    }
}
