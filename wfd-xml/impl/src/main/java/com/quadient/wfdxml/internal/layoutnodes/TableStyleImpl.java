package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.TableStyle;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class TableStyleImpl extends NodeImpl<TableStyle> implements TableStyle {

    @Override
    public String getXmlElementName() {
        return "TableStyle";
    }

    @Override
    public void export(XmlExporter exporter) { }
}
