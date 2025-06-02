package com.quadient.wfdxml.internal.layoutnodes.support;

import com.quadient.wfdxml.api.layoutnodes.TabulatorType;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

public class TabulatorProperties implements XmlExportable {
    private final List<Tabulator> tabulators = new ArrayList<>();

    private double defaultTab = 0.0125;
    private boolean useOutsideTabs = false;

    public TabulatorProperties addTabulator(double pos, TabulatorType type) {
        Tabulator tabulator = new Tabulator(pos, type);
        tabulators.add(tabulator);
        return this;
    }

    public TabulatorProperties addDecimalTabulatorWithPoint(double pos, String point) {
        Tabulator tabulator = new Tabulator(pos, point);
        tabulators.add(tabulator);
        return this;
    }

    public TabulatorProperties setDefaultTab(double defaultTab) {
        this.defaultTab = defaultTab;
        return this;
    }

    public TabulatorProperties setUseOutsideTabs(boolean useOutsideTabs) {
        this.useOutsideTabs = useOutsideTabs;
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("TabulatorProperties")
                .addElementWithDoubleData("Default", defaultTab)
                .addElementWithBoolData("UseOutsideTabs", useOutsideTabs);

        for (Tabulator tabulator : tabulators) {
            tabulator.export(exporter);
        }
        exporter.endElement();
    }
}