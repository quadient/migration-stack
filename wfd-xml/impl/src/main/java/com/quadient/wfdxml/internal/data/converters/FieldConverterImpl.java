package com.quadient.wfdxml.internal.data.converters;

import com.quadient.wfdxml.api.data.converters.FieldConverter;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.typeToString;

public abstract class FieldConverterImpl<S extends FieldConverterImpl<S>> implements XmlExportable, FieldConverter {
    protected NodeType inputType = STRING;
    protected NodeType outputType = STRING;

    public NodeType getInputType() {
        return inputType;
    }

    @SuppressWarnings("unchecked")
    public S setInputType(NodeType inputType) {
        this.inputType = inputType;
        return (S) this;
    }

    public NodeType getOutputType() {
        return outputType;
    }

    @SuppressWarnings("unchecked")
    public S setOutputType(NodeType outputType) {
        this.outputType = outputType;
        return (S) this;
    }

    abstract String getFieldConverterClassNameForExport();

    abstract void exportFieldConverterSettings(XmlExporter exporter);

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("FCV")
                .addStringAttribute("FCVClassName", getFieldConverterClassNameForExport())
                .beginElement("FCVProps")
                .addElementWithStringData("InputType", typeToString(inputType))
                .addElementWithStringData("OutputType", typeToString(outputType));

        exportFieldConverterSettings(exporter);

        exporter.endElement() //FCVProps
                .endElement();//FCV
    }
}
