package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.examples.data.XmlDataInputSettings;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.FlowArea;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput;

import static com.quadient.wfdxml.api.layoutnodes.data.DataType.BOOL;
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.DOUBLE;
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.INT;
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.INT64;
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.OPTIONAL;
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.STRING;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.JOB_NAME;
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.CALCULATED;
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.CONSTANT;
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.DISCONNECTED;
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.EXTERNAL_CALCULATED;
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.GLOBAL_VARIABLE;

public class VariableUsageExample extends WorkflowUsageExample {

    private final String dataFilePath;

    private static final String DEFAULT_MOCK_FILE_PATH = "C://tmp/Data.xml";

    public VariableUsageExample() {
        this(DEFAULT_MOCK_FILE_PATH);
    }

    public VariableUsageExample(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {

        XmlDataInputSettings.createScriptXmlDataInput(builder);

        XmlDataInput xmlDataInput = XmlDataInputSettings.createXmlDataInput(builder, dataFilePath);
        Layout layout = builder.addLayout()
                .setName("MyLayout");
        builder.connectModules(xmlDataInput, 0, layout, 0);

        XmlDataInputSettings.importDataStructureIntoLayout(xmlDataInput, layout);

        Page page = layout.addPage();
        return new ExampleContent(layout, page);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createVariableFlowArea(exampleContent.layout(), exampleContent.page());
    }

    private void createVariableFlowArea(Layout layout, Page page) {
        Variable systemVariable = layout.getData()
                .getSystemVariable(JOB_NAME);

        Variable stringConstant = layout.getData()
                .addVariable()
                .setName("Constant")
                .setValue("StringValue");

        Variable numberingVariable = layout.getData()
                .addVariable()
                .setValue(4)
                .setKind(CONSTANT)
                .setDataType(INT)
                .setName("NumberingConstant");

        Variable doubleConstant = layout.getData()
                .addVariable()
                .setName("DoubleConstant")
                .setKind(CONSTANT)
                .setDataType(DOUBLE)
                .setValue(0.0054);

        Variable globalVariable = layout.getData()
                .addVariable()
                .setName("GlobalVariable")
                .setKind(GLOBAL_VARIABLE)
                .setDataType(BOOL)
                .setValue(false);

        Variable disconnectedVariable = layout.getData()
                .addVariable()
                .setName("DisconnectedVariable")
                .setKind(DISCONNECTED)
                .setDataType(INT64)
                .setValue(1567L);

        Variable xmlDataInputVariable = layout.getData()
                .findVariable("XmlElement2", "Data2");

        Variable urlVariable = layout.getData()
                .addVariable()
                .setValue("https://www.quadient.com/")
                .setName("HyperLinkVariable");

        Variable variableWithScript = layout.getData()
                .addVariable()
                .setKind(CALCULATED)
                .setDataType(INT)
                .setScript("Script")
                .setName("VariableWithScript");

        Variable variableWithExternalScript = layout.getData()
                .addVariable()
                .setKind(EXTERNAL_CALCULATED)
                .setDataType(STRING)
                .setValue("C://tmp2")
                .setName("VariableWithExternalScript");

        Variable optional = layout.getData()
                .addVariable()
                .setKind(CONSTANT)
                .setDataType(OPTIONAL)
                .setName("Optional");

        Flow variableFlow = layout.addFlow()
                .setName("VariableFlow")
                .addParagraph()
                .addText()
                .appendText("SystemVariable: ")
                .appendVariable(systemVariable)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("StringConstant: ")
                .appendVariable(stringConstant)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("NumberingVariable: ")
                .appendVariable(numberingVariable)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("DoubleConstant: ")
                .appendVariable(doubleConstant)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("GlobalVariable: ")
                .appendVariable(globalVariable)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("DisconnectedVariable")
                .appendVariable(disconnectedVariable)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("Variable from XmlDataInput: ")
                .appendVariable(xmlDataInputVariable)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("Variable with link: ")
                .appendVariable(urlVariable)
                .back()
                .back();

        FlowArea topRightArea = page.addFlowArea()
                .setPositionAndSize(0.110, 0.005, 0.095, 0.06)
                .setFlow(variableFlow)
                .setName("TopRightArea");
    }
}
