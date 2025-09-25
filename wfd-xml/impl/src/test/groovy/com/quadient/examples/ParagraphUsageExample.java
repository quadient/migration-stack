package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.*;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.api.module.Layout;

import java.nio.file.Paths;

import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.AlignType.JUSTIFY_lEFT;
import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.LEFT;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.CornerType.ROUND_OUT;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_BORDERS;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_LINES;

public class ParagraphUsageExample extends WorkflowUsageExample {

    private static final String DEFAULT_MOCK_FONT_PATH = "C://tmp/Centaur.TTF";

    private final String fontPath;

    public ParagraphUsageExample() {
        this(DEFAULT_MOCK_FONT_PATH);
    }

    public ParagraphUsageExample(String fontPath) {
        this.fontPath = fontPath;
    }

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        Layout layout = builder.addLayout();
        Page firstPage = layout.addPage()
                .setName("FirstPage");

        return new ExampleContent(layout, firstPage);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createParagraphFlowArea(exampleContent.layout(), exampleContent.page());
    }

    private void createParagraphFlowArea(Layout layout, Page page) {
        Color green = layout.addColor()
                .setRGB(0.0, 1.0, 0.0)
                .setName("Green");

        Color blue = layout.addColor()
                .setRGB(0, 0, 255)
                .setName("Blue");

        Color gold = layout.addColor()
                .setRGB(255, 204, 0)
                .setName("Gold");

        Font centaurFont = layout.addFont().setName(Paths.get(fontPath).getFileName().toString());
        centaurFont.addSubfont().setLocation(fontPath, LocationType.DISK);

        Variable urlVariable = layout.getData()
                .addVariable()
                .setValue("https://www.quadient.com/")
                .setName("URLVariable");

        FillStyle greenFillStyle = layout.addFillStyle()
                .setColor(green)
                .setName("GreenFillStyle");
        FillStyle blueFillStyle = layout.addFillStyle()
                .setColor(blue)
                .setName("BlueFillStyle");
        FillStyle goldFillStyle = layout.addFillStyle()
                .setColor(gold)
                .setName("GoldFillStyle");
        FillStyle blackFillStyle = layout.addFillStyle();

        BorderStyle textStyleBorderStyle = layout.addBorderStyle()
                .setName("TextStyleBorderStyle")
                .select(ALL_BORDERS)
                .setLineWidth(0.0003)
                .setLineFillStyle(greenFillStyle)
                .backToBorderStyle()
                .setOffsetBottom(0.0025);

        BorderStyle flowAreaBorderStyle = layout.addBorderStyle()
                .setName("FlowAreaBorderStyle")
                .select(ALL_LINES)
                .setLineFillStyle(blackFillStyle)
                .setLineWidth(0.00045)
                .backToBorderStyle()
                .setMarginBottom(0.0023);

        BorderStyle paragraphStyleBorderStyle = layout.addBorderStyle()
                .setName("ParagraphStyleBorderStyle")
                .setMargins(0.0005, 0.0005, 0.0005, 0)
                .setOffsetLeft(0.002)
                .select(ALL_BORDERS)
                .setLineWidth(0.0005)
                .setCorner(ROUND_OUT)
                .setRadiusX(0.005)
                .setRadiusY(0.005)
                .setLineFillStyle(blackFillStyle)
                .backToBorderStyle()
                .setFill(goldFillStyle);

        TextStyle normalTextStyle = layout.addTextStyle()
                .setName("Normal");

        TextStyle coloredTextStyle = layout.addTextStyle()
                .setFillStyle(greenFillStyle)
                .setName("ColoredTextStyle");

        TextStyle largeTextStyle = layout.addTextStyle()
                .setFontSize(17)
                .setName("LargeTextStyle");

        TextStyle superScriptTextStyle = layout.addTextStyle()
                .setBorderStyle(textStyleBorderStyle)
                .setFontSize(17)
                .setSuperScript(true)
                .setName("SuperScriptTextStyle");

        TextStyle styledTextStyle = layout.addTextStyle()
                .setFillStyle(blueFillStyle)
                .setFont(centaurFont)
                .seItalic(true)
                .setBold(true)
                .setUnderline(true)
                .setLanguage("cs")
                .setName("StyledTextStyle");

        TextStyle hyperLinkTextStyle = layout.addTextStyle()
                .setFillStyle(blueFillStyle)
                .setUnderline(true)
                .setUrlTarget(urlVariable);

        ParagraphStyle normalParagraphStyle = layout.addParagraphStyle()
                .setTextStyle(normalTextStyle)
                .setName("Normal");

        ParagraphStyle tabulatorParagraphStyle = layout.addParagraphStyle()
                .setTextStyle(normalTextStyle)
                .setDefaultTabSize(0.017)
                .addTabulator(0.025, LEFT)
                .setName("TabulatorStyledParagraphStyle");

        ParagraphStyle styledParagraphStyle = layout.addParagraphStyle()
                .setTextStyle(normalTextStyle)
                .setAlignType(JUSTIFY_lEFT)
                .setLeftIndent(0.001)
                .setRightIndent(0.005)
                .setFirstLineLeftIndent(0.01)
                .setSpaceBeforeFirstPara(true)
                .setSpaceBefore(0.001)
                .setSpaceAfter(0.002)
                .setName("StyledParagraphStyle");

        ParagraphStyle styledParagraphStyleWithBorderStyle = layout.addParagraphStyle()
                .setTextStyle(normalTextStyle)
                .setAlignType(JUSTIFY_lEFT)
                .setLeftIndent(0.001)
                .setRightIndent(0.005)
                .setFirstLineLeftIndent(0.01)
                .setSpaceBeforeFirstPara(true)
                .setSpaceBefore(0.001)
                .setSpaceAfter(0.002)
                .setBorderStyle(paragraphStyleBorderStyle)
                .setName("StyledParagraphStyleWithBorderStyle");

        Flow insertedFlow = layout.addFlow().setName("InsertedFlow")
                .addParagraph()
                .addText()
                .appendText("InsertedFlow with link: ")
                .back()
                .addText()
                .setTextStyle(hyperLinkTextStyle)
                .appendText("Quadient")
                .back()
                .back();

        Flow textFlow = layout.addFlow()
                .setName("TextFlow")
                .addParagraph()
                .addText()
                .appendText("NormalText")
                .back().back()
                .addParagraph()
                .addText()
                .appendText("ColoredText")
                .setTextStyle(coloredTextStyle)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("StyledTextWithCentaurFont")
                .setTextStyle(styledTextStyle)
                .back()
                .back()
                .addParagraph()
                .addText()
                .appendText("LargeText")
                .setTextStyle(largeTextStyle)
                .back()
                .addText()
                .appendText("With SuperScript")
                .setTextStyle(superScriptTextStyle)
                .back()
                .addText()
                .appendText("\n")
                .setTextStyle(normalTextStyle)
                .back()
                .back()
                .addParagraph()
                .setParagraphStyle(styledParagraphStyle)
                .addText()
                .appendText("Styled ParagraphStyle by normal text, center left alignment," +
                        " indent on first, space before and after and next attributes")
                .back()
                .back()
                .addParagraph()
                .addText()
                .setTextStyle(styledTextStyle)
                .appendText("Styled ParagraphStyle by StyledTextWithCentaurFont")
                .back()
                .setParagraphStyle(styledParagraphStyleWithBorderStyle)
                .back()
                .addParagraph()
                .setParagraphStyle(tabulatorParagraphStyle)
                .addText()
                .setTextStyle(normalTextStyle)
                .appendText("\tNew ParagraphStyle with left tabulator, \nsoft line end  and \tdefault tabulator")
                .back().back()
                .addParagraph()
                .addText()
                .appendText("Flow in flow: ")
                .appendFlow(insertedFlow)
                .back()
                .back();

        FlowArea flowArea = page.addFlowArea()
                .setPositionAndSize(0.005, 0.005, 0.095, 0.06)
                .setFlow(textFlow)
                .setBorderStyle(flowAreaBorderStyle)
                .setName("TopLeftArea");
    }
}
