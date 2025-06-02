package com.quadient.wfdxml.internal.module.xmldatainput;

import com.quadient.wfdxml.api.module.xmldatainput.LocationType;
import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput;
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition;
import com.quadient.wfdxml.internal.data.converters.FieldConverterImpl;
import com.quadient.wfdxml.internal.module.WorkFlowModuleImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.api.module.xmldatainput.LocationType.DISK_LOCATION;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.FLATTEN;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.IGNORE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ATTRIBUTE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ELEMENT;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.PC_DATA;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.ARRAY;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.OPTIONAL;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE;
import static java.util.Collections.singletonList;

public class XmlDataInputImpl extends WorkFlowModuleImpl<XmlDataInput> implements XmlDataInput {
    private LocationType locationType;
    private String filePath;
    private boolean createByScript = false;
    private String script;
    private XmlNodeImpl xmlNode;
    private boolean allowAnyOrderOfElements = false;

    @Override
    public XmlDataInputImpl setFileLocation(LocationType type, String filePath) {
        this.locationType = type;
        this.filePath = filePath;
        return this;
    }

    @Override
    public XmlDataInputImpl setDiskLocation(String location) {
        setFileLocation(DISK_LOCATION, location);
        return this;
    }

    @Override
    public XmlDataInputImpl setCreateByScript(boolean createByScript) {
        this.createByScript = createByScript;
        return this;
    }

    @Override
    public XmlDataInputImpl setScript(String script) {
        if (script != null && !script.isEmpty()) {
            createByScript = true;
        }
        this.script = script;
        return this;
    }

    @Override
    public XmlNodeImpl addRootXmlNode() {
        if (xmlNode != null) {
            throw new IllegalStateException("It is not possible to add two XmlNodes directly into XmlDataInput");
        }
        xmlNode = new XmlNodeImpl()
                .setName("Root").setType(ELEMENT).setOptionality(ONE).setChange(FLATTEN).setXmlName("Root");
        return xmlNode;
    }

    public XmlNodeImpl getXmlNode() {
        return xmlNode;
    }

    @Override
    public XmlDataInputImpl allowAnyOrderOfElements(boolean allowAnyOrderOfElements) {
        this.allowAnyOrderOfElements = allowAnyOrderOfElements;
        return this;
    }

    @Override
    public WorkFlowTreeDefinition generateDataDefinition() {
        WorkFlowTreeDefinition root = new WorkFlowTreeDefinition("Root", SUB_TREE, ARRAY);
        if (xmlNode == null) {
            return root;
        }
        root.addSubNodes(convertToWtd(xmlNode.getXmlNodes()));
        return root;
    }

    private List<WorkFlowTreeDefinition> convertToWtd(List<XmlNodeImpl> xmlNodes) {
        List<WorkFlowTreeDefinition> result = new ArrayList<>();
        for (XmlNodeImpl xmlNode : xmlNodes) {
            result.addAll(convertToWtd(xmlNode));
        }
        return result;
    }

    private List<WorkFlowTreeDefinition> convertToWtd(XmlNodeImpl x) {
        boolean ignore = x.getChange() == IGNORE;

        if (x.getChange() != FLATTEN && !ignore) {
            WorkFlowTreeDefinition nd = new WorkFlowTreeDefinition();
            nd.setCaption(x.getName());
            //if conversion - conversions are not supported
            if (x.getType() == ATTRIBUTE || x.getType() == PC_DATA) {
                FieldConverterImpl fieldConverter = x.getFieldConverter();
                assertFieldConverterInputTypeIsString(fieldConverter);
                nd.setNodeType(fieldConverter.getOutputType());
                nd.setNodeOptionality(MUST_EXIST);
            } else {
                nd.setNodeType(SUB_TREE);
                switch (x.getOptionality()) {
                    case ONE:
                        nd.setNodeOptionality(MUST_EXIST);
                        break;
                    case ZERO_OR_ONE:
                        nd.setNodeOptionality(OPTIONAL);
                        break;
                    case ZERO_OR_MORE:
                    case ONE_OR_MORE:
                        nd.setNodeOptionality(ARRAY);
                        break;
                }
            }
            nd.getSubNodes().addAll(convertToWtd(x.getXmlNodes()));
            return singletonList(nd);
        }

        return convertToWtd(x.getXmlNodes());
    }

    private void assertFieldConverterInputTypeIsString(FieldConverterImpl fieldConverter) {
        if (fieldConverter.getInputType() != STRING) {
            throw new IllegalArgumentException("Field convertor inside XmlNode must have input type 'STRING'. But had '" + fieldConverter.getInputType() + "'");
        }
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.beginElement("XMLDataInput")
                .addElementWithIface("Id", this)
                .addElementWithStringData("Name", getName())
                .beginElement("ModulePos")
                .addIntAttribute("X", getPosX())
                .addIntAttribute("Y", getPosY())
                .endElement();
        if (filePath != null) {
            exporter.addElementWithStringData("Location", convertLocationTypeToXmlName() + "," + filePath);
        }
        if (xmlNode != null) {
            xmlNode.export(exporter);
        }

        exporter.addElementWithBoolData("AllowAnyOrderOfElements", allowAnyOrderOfElements);

        exporter.addElementWithBoolData("CreateByScript", createByScript);
        if (createByScript) {
            exporter.addElementWithStringData("Script", script);
        }

        exporter.endElement();
    }

    private String convertLocationTypeToXmlName() {
        switch (locationType) {
            case UNKNOWN_LOCATION:
                return "UnknownLocation";
            case DISK_LOCATION:
                return "DiskLocation";
            case VCS_LOCATION:
                return "VCSLocation";
            default:
                throw new IllegalStateException(locationType.toString());
        }
    }
}