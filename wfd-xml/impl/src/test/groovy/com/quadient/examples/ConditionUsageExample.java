package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.data.DataType;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind;
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet;
import com.quadient.wfdxml.api.module.Layout;

import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.MULTIPLE_ROWS;

public class ConditionUsageExample extends WorkflowUsageExample {

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        Layout layout = builder.addLayout()
                .setName("MyLayout");
        Page conditionPage = layout.addPage()
                .setName("ConditionPage");
        return new ExampleContent(layout, conditionPage);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createFlowAreaAndSetCondition(exampleContent.layout(), exampleContent.page());
    }

    private void createFlowAreaAndSetCondition(Layout layout, Page page) {
        Flow conditionFlow = layout.addFlow()
                .setName("ConditionFlow");

        createFlowWithCondition(layout, conditionFlow);
        createRowSetWithCondition(layout, conditionFlow);

        page.addFlowArea().setName("ReallyBottomLeftArea")
                .setPositionAndSize(0.005, 0.225, 0.095, 0.045)
                .setFlow(conditionFlow);
    }

    private void createFlowWithCondition(Layout layout, Flow flow) {
        Variable boolVariable = layout.getData().addVariable()
                .setDataType(DataType.BOOL)
                .setKind(VariableKind.CONSTANT)
                .setValue(true)
                .setName("BoolVariable");

        Flow resultingFlow = layout.addFlow().addParagraph().addText()
                .appendText("This FLow is shown in Proof only when BoolVariable is True\n").back().back()
                .setName("ResultingFlow");

        Flow resultingFlow2 = layout.addFlow().addParagraph().addText()
                .appendText("This Flow is shown in Proof when script return True \n ").back().back()
                .setName("ResultingFlow");

        Flow selectByConditionFlow = layout.addFlow()
                .addLineForSelectByCondition(boolVariable, resultingFlow)
                .setType(Flow.Type.SELECT_BY_CONDITION)
                .setName("SelectedByConditionFlow");

        Flow selectByInlineConditionFlow = layout.addFlow()
                .setType(Flow.Type.SELECT_BY_INLINE_CONDITION)
                .addLineForSelectByInlineCondition("return false;", resultingFlow2)
                .setName("SelectedByInlineConditionFlow");

        flow.addParagraph().addText()
                .appendFlow(selectByConditionFlow);
        flow.addParagraph().addText()
                .appendFlow(selectByInlineConditionFlow);
    }

    private void createRowSetWithCondition(Layout layout, Flow conditionFlow) {
        Variable falseBoolVariable = layout.getData().addVariable()
                .setDataType(DataType.BOOL)
                .setKind(VariableKind.CONSTANT)
                .setValue(false)
                .setName("BoolVariable2");

        Flow cellContent = layout.addFlow()
                .addParagraph()
                .addText()
                .appendText("This RowSet is shown when BoolVariable2 is True")
                .back().back();

        Flow cellContent2 = layout.addFlow()
                .addParagraph()
                .addText()
                .appendText("This RowSet is shown when script is True ")
                .back().back();

        Flow normalCellContent = layout.addFlow()
                .addParagraph()
                .addText()
                .appendText("This RowSet is shown")
                .back().back();

        RowSet rowSet = layout.addRowSet()
                .addCell(layout.addCell()
                        .setFlow(cellContent));

        RowSet rowSet2 = layout.addRowSet()
                .addCell(layout.addCell()
                        .setFlow(cellContent2));

        conditionFlow
                .addParagraph()
                .addText()
                .appendTable(
                        layout.addTable().setName("Condition Table")
                                .addColumn()
                                .setRowSet(layout.addRowSet().setType(MULTIPLE_ROWS)
                                        .addRowSet(
                                                layout.addRowSet()
                                                        .addCell(
                                                                layout.addCell()
                                                                        .setFlow(normalCellContent)
                                                                        .setName("Normal RowSet")
                                                        )
                                        )
                                        .addRowSet(
                                                layout.addRowSet()
                                                        .setType(RowSet.Type.SELECT_BY_CONDITION)
                                                        .addLineForSelectByCondition(falseBoolVariable, rowSet)
                                                        .setName("SelectByCondition RowSet")
                                        )
                                        .addRowSet(
                                                layout.addRowSet()
                                                        .setType(RowSet.Type.SELECT_BY_INLINE_CONDITION)
                                                        .addLineForSelectByInlineCondition("return true;", rowSet2)
                                                        .setName("SelectByInlineCondition RowSet")
                                        )
                                        .addRowSet(
                                                layout.addRowSet()
                                                        .addCell(layout.addCell()
                                                                .setFlow(normalCellContent)
                                                                .setName("Normal RowSet"))
                                        )
                                ).setName("MultipleRow")
                ).back().back();
    }
}
