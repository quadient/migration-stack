package com.quadient.wfdxml.internal

import com.quadient.wfdxml.internal.xml.export.XmlExporter

class TestNode extends NodeImpl<TestNode> {

    @Override
    void export(XmlExporter exporter) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    String toString() {
        return "TestNode{'$name', '$comment'} @${System.identityHashCode(this)}"
    }
}