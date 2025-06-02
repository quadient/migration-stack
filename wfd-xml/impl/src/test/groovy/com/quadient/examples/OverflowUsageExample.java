package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.examples.data.XmlDataInputSettings;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.data.DataType;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind;
import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput;

import static com.quadient.wfdxml.api.layoutnodes.Pages.PageConditionType.SELECT_BY_CONDITION;
import static com.quadient.wfdxml.api.layoutnodes.Pages.PageOrder.VARIABLE_SELECTION;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.OVERFLOW;
import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.REPEATED;

public class OverflowUsageExample extends WorkflowUsageExample {

    private final String dataFilePath;

    private static final String DEFAULT_MOCK_DATA_FILE_PATH = "C://tmp/Data.xml";

    public OverflowUsageExample() {
        this(DEFAULT_MOCK_DATA_FILE_PATH);
    }

    public OverflowUsageExample(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        XmlDataInput xmlDataInput = XmlDataInputSettings.createXmlDataInput(builder, dataFilePath);
        Layout layout = builder.addLayout()
                .setName("MyLayout");
        builder.connectModules(xmlDataInput, 0, layout, 0);

        XmlDataInputSettings.importDataStructureIntoLayout(xmlDataInput, layout);

        Page overflowPage = layout.addPage()
                .setName("OverflowPage");

        return new ExampleContent(layout, overflowPage);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createFlowAreaAndSetOverflowSettings(exampleContent.layout(), exampleContent.page());
    }

    private void createRepeatedFlow(Layout layout, Flow overflowingFlow) {
        Variable repeated = layout.getData()
                .addVariable()
                .setKind(VariableKind.CONSTANT)
                .setDataType(DataType.INT)
                .setValue(3);
        Flow repeatedFlow = layout.addFlow()
                .setType(Flow.Type.REPEATED)
                .setVariable(repeated)
                .addParagraph()
                .addText()
                .appendText("This flow is repeated 3 times \n")
                .back()
                .back();
        overflowingFlow.addParagraph()
                .addText()
                .appendFlow(repeatedFlow)
                .back()
                .back();
    }

    private void createRepeatedRowSet(Layout layout, Flow overflowingFlow) {
        Flow cellContent = layout.addFlow()
                .addParagraph()
                .addText()
                .appendText("Person name: ")
                .appendVariable(layout.getData()
                        .findVariable("Person", "Name")
                ).back()
                .back();

        overflowingFlow.addParagraph()
                .addText()
                .appendText("Repeated table row, which oveflow to next page:").back().back()
                .addParagraph()
                .addText()
                .appendTable(
                        layout.addTable()
                                .setName("Repeated Table")
                                .addColumn()
                                .setRowSet(
                                        layout.addRowSet()
                                                .setType(REPEATED)
                                                .setVariable(
                                                        layout.getData()
                                                                .findVariable("Person")
                                                )
                                                .setRowSet(layout.addRowSet()
                                                        .addCell(layout.addCell()
                                                                .setFlow(cellContent)
                                                        )
                                                ).setName("Repeated RowSet")
                                )
                ).back()
                .back();
    }

    private Flow createOverflowingFlow(Layout layout) {
        Flow overflowingFlow = layout.addFlow();
        createRepeatedFlow(layout, overflowingFlow);
        createRepeatedRowSet(layout, overflowingFlow);
        return overflowingFlow;
    }

    private void createFlowAreaAndSetOverflowSettings(Layout layout, Page overflowPage) {
        layout.getPages()
                .setPageOrder(VARIABLE_SELECTION)
                .setStartPage(overflowPage);

        Variable overflowVariable = layout.getData()
                .getSystemVariable(OVERFLOW);

        overflowPage.setType(SELECT_BY_CONDITION)
                .addLineForSelectByCondition(overflowVariable, 0, overflowPage);

        Flow overflowingFlow = createOverflowingFlow(layout);

        overflowPage.addFlowArea()
                .setName("BottomLeftArea")
                .setPositionAndSize(0.005, 0.195, 0.095, 0.025)
                .setFlowToNextPage(true)
                .setFlow(overflowingFlow);
    }
}