package com.quadient.examples.data;

import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.data.DataDefinition;
import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.quadient.wfdxml.api.data.converters.Converters.custCodeConverter;
import static com.quadient.wfdxml.api.data.converters.Converters.doubleConverter;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.FLATTEN;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.NONE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE_OR_MORE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ZERO_OR_ONE;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ELEMENT;
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.PC_DATA;
import static java.nio.charset.StandardCharsets.UTF_8;

public class XmlDataInputSettings {

    private static final String XML_DATA_FILE_RESOURCE = "com/quadient/wfdxml/input/data.xml";

    public static String getXmlData() throws IOException {
        return new String(
                XmlDataInputSettings.class.getClassLoader()
                        .getResourceAsStream(XML_DATA_FILE_RESOURCE)
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    public static XmlDataInput createXmlDataInput(WfdXmlBuilder builder, String dataLocationPath) {
        XmlDataInput xmlDataInput = builder.addXmlDataInput()
                .setDiskLocation(dataLocationPath)
                .setName("MyXmlDataInput");
        // @formatter:off
        xmlDataInput
                .addRootXmlNode().setXmlName("RootNode")
                     .addSubNode().setName("XmlElement1").setType(ELEMENT).setOptionality(ONE).setChange(NONE).setXmlName("XmlElement1")
                        .addSubNode().setName("Data1").setType(ELEMENT).setOptionality(ONE).setChange(FLATTEN).setXmlName("Data1")
                            .addSubNode().setName("Data1").setType(PC_DATA).setOptionality(ONE).setChange(NONE).setXmlName("#PCData")
                            .getParent()
                        .getParent()
                    .getParent()
                    .addSubNode("XmlElement2", ELEMENT, ZERO_OR_ONE, NONE, "XmlElement2")
                        .addSubNode("Data2", ELEMENT, ONE, FLATTEN, "Data2")
                            .addSubNode("Data2", PC_DATA, ONE, NONE, "#PCData", doubleConverter().setDecimalPoint("p"))
                            .getParent()
                        .getParent()
                    .getParent()
                    .addSubNode("XmlElement3", ELEMENT, ZERO_OR_ONE, NONE, "XmlElement2")
                        .addSubNode("Data3", ELEMENT, ONE, FLATTEN, "Data3")
                            .addSubNode("Data3", PC_DATA, ONE, NONE, "#PCData",
                                    custCodeConverter().addCase("In", "Out"))
                            .getParent()
                        .getParent()
                    .getParent()
                    .addSubNode("Persons", ELEMENT, ONE, FLATTEN, "Persons")
                        .addSubNode("Person", ELEMENT, ONE_OR_MORE, NONE, "Person")
                            .addSubNode("Name", ELEMENT, ONE, FLATTEN, "Name")
                                .addSubNode("Name", PC_DATA, ONE, NONE, "#PCData")
                                .getParent()
                            .getParent()
                        .getParent()
                    .getParent()
                .getParent();
        // @formatter:on
        return xmlDataInput;
    }

    public static void createScriptXmlDataInput(WfdXmlBuilder builder) {
        builder.addXmlDataInput()
                .setName("CreatedByScript")
                .setCreateByScript(true)
                .setScript("Sample\r\nScript")
                .setPosX(15).setPosY(20);
    }

    public static void importDataStructureIntoLayout(XmlDataInput xmlDataInput, Layout layout) {
        DataDefinition dataDefinition = xmlDataInput.generateDataDefinition();
        layout.getData().importDataDefinition(dataDefinition);
    }
}
