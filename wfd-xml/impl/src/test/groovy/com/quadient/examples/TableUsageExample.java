package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.FlowArea;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.api.module.Layout;

import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.CornerType.CUT_OUT;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_LINES;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.UPPER_RIGHT_CORNER;
import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.MULTIPLE_ROWS;

public class TableUsageExample extends WorkflowUsageExample {

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        Layout layout = builder.addLayout().setName("Layout");
        Page firstPage = layout.addPage().setName("FirstPage");
        return new ExampleContent(layout, firstPage);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createTableFlowArea(exampleContent.layout(), exampleContent.page());
    }

    public static void createTableFlowArea(Layout layout, Page page) {
        Flow tableFlow = layout.addFlow()
                .setName("TableFlow");

        Flow cellFlow = layout.addFlow()
                .setName("Cell Flow")
                .addParagraph()
                .addText()
                .appendText("Cell Flow")
                .back()
                .back();

        Flow specialCorner = layout.addFlow()
                .setName("Special Cell Corner")
                .addParagraph()
                .addText()
                .appendText("Special Cell Corner")
                .back()
                .back();

        BorderStyle roundCorner = layout.addBorderStyle()
                .setName("Round Corner")
                .select(UPPER_RIGHT_CORNER)
                .setLineFillStyle(layout.addFillStyle()
                        .setColor(layout.addColor()
                                .setRGB(128, 0, 255)
                        ).setName("CornerColor"))
                .setLineWidth(0.0008)
                .setCorner(CUT_OUT)
                .setRadiusX(0.005)
                .setRadiusY(0.0035)
                .backToBorderStyle();

        tableFlow.addParagraph()
                .addText()
                .appendText("Simple table:");
        tableFlow.addParagraph()
                .addText()
                .appendTable(layout.addTable()
                        .setName("Simple")
                        .setBorderStyle(
                                layout.addBorderStyle()
                                        .setName("Simple")
                                        .select(ALL_LINES)
                                        .setLineFillStyle(layout.addFillStyle())
                                        .backToBorderStyle()
                                        .setMarginBottom(0.002)
                        )
                        .addColumn()
                        .addColumn()
                        .setRowSet(layout.addRowSet()
                                .setType(MULTIPLE_ROWS)
                                .addRowSet(layout.addRowSet()
                                        .addCell(
                                                layout.addCell()
                                                        .setFlow(cellFlow)
                                        )
                                        .addCell(
                                                layout.addCell()
                                                        .setFlow(specialCorner)
                                                        .setBorderStyle(roundCorner)
                                        )
                                )
                                .addRowSet(layout.addRowSet()
                                        .addCell(
                                                layout.addCell()
                                                        .setFlow(cellFlow)
                                        )
                                        .addCell(
                                                layout.addCell()
                                                        .setFlow(cellFlow)
                                        )
                                )
                        )
                );

        tableFlow.addParagraph()
                .addText()
                .appendText("HeaderFooter table:");
        tableFlow.addParagraph()
                .addText()
                .appendTable(
                        layout.addTable().setName("HeaderFooter")
                                .addColumn()
                                .addColumn()
                                .setRowSet(layout.addRowSetHeaderFooter()
                                        .setFirstHeader(layout.addRowSet()
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow))
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                        )
                                        .setHeader(layout.addRowSet()
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                        )
                                        .setBody(layout.addRowSet()
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                        )
                                        .setFooter(layout.addRowSet()
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                                .addCell(
                                                        layout.addCell()
                                                                .setFlow(cellFlow)
                                                )
                                        )
                                        .setLastFooter(
                                                layout.addRowSet()
                                                        .addCell(layout.addCell()
                                                                .setFlow(cellFlow)
                                                        )
                                                        .addCell(
                                                                layout.addCell()
                                                                        .setFlow(cellFlow)
                                                        )
                                        )
                                )
                );

        FlowArea middleRightArea = page.addFlowArea()
                .setPositionAndSize(0.110, 0.07, 0.095, 0.06)
                .setFlow(tableFlow)
                .setName("MiddleRightArea");
    }
}
