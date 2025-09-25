package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.*;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.module.Layout;

import java.nio.file.Paths;

import static com.quadient.wfdxml.api.layoutnodes.NumberedList.ListConversionType.REPEATED_ALPHABETIC;
import static com.quadient.wfdxml.api.layoutnodes.NumberedList.ListConversionType.ROMAN_NUMBER;
import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.NumberingType.RESET;
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.INT;
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.NUMBERING;

public class ListsUsageExample extends WorkflowUsageExample {

    private static final String DEFAULT_MOCK_FONT_PATH = "C://tmp/Wingdings.TTF";

    private final String fontPath;

    public ListsUsageExample() {
        this(DEFAULT_MOCK_FONT_PATH);
    }

    public ListsUsageExample(String fontPath) {
        this.fontPath = fontPath;
    }

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        Layout layout = builder.addLayout()
                .setName("Layout");
        Page overflowPage = layout.addPage()
                .setName("FirstPage");
        return new ExampleContent(layout, overflowPage);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createListsFlowArea(exampleContent.layout(), exampleContent.page());
    }

    private void createListsFlowArea(Layout layout, Page page) {
        Flow listFlow = layout.addFlow()
                .setName("ListFlow");

        ParagraphStyle normalParagraphStyle = layout.addParagraphStyle();
        TextStyle normalTextStyle = layout.addTextStyle();

        addBulletList(layout, listFlow);
        listFlow.addParagraph()
                .setParagraphStyle(normalParagraphStyle);

        addNumberingList(layout, listFlow, normalTextStyle);
        listFlow.addParagraph()
                .setParagraphStyle(normalParagraphStyle);

        addNumberingLevelingList(layout, listFlow, normalTextStyle);
        listFlow.addParagraph()
                .setParagraphStyle(normalParagraphStyle);

        addNumberingRomanList(layout, listFlow, normalTextStyle);
        listFlow.addParagraph()
                .setParagraphStyle(normalParagraphStyle);
        addAlphabeticNumberedList(layout, listFlow, normalTextStyle);

        FlowArea middleLeftArea = page.addFlowArea()
                .setPositionAndSize(0.005, 0.07, 0.095, 0.12)
                .setFlow(listFlow)
                .setName("MiddleLeftArea");
    }

    private void addAlphabeticNumberedList(Layout layout, Flow flow, TextStyle textStyle) {
        Variable variable1 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("AlphabeticLevel1");

        Variable variable2 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("AlphabeticLevel2");

        Variable variable3 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("AlphabeticLevel3");

        Variable variable4 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("AlphabeticLevel4");

        NumberedList numberedList = layout.addNumberedList()
                .setListConversion(REPEATED_ALPHABETIC);

        ParagraphStyle alphaParagraphStyleLevel1 = numberedList.addIndentLevel(variable1, textStyle, ".")
                .setDefaultTabSize(0.002)
                .setName("AlphaParagraphStyleLevel1");

        ParagraphStyle alphaParagraphStyleLevel2 = numberedList.addIndentLevel(variable2, textStyle, ".")
                .setFirstLineLeftIndent(0.005)
                .setName("AlphaParagraphStyleLevel2");

        ParagraphStyle alphaParagraphStyleLevel3 = numberedList.addIndentLevel(variable3, textStyle, ".")
                .setFirstLineLeftIndent(0.010)
                .setDefaultTabSize(0.005)
                .setName("AlphaParagraphStyleLevel3");

        ParagraphStyle alphaParagraphStyleLevel4 = numberedList.addIndentLevel(variable4, textStyle, ")")
                .setFirstLineLeftIndent(0.015)
                .setDefaultTabSize(0.005)
                .setName("AlphaParagraphStyleLevel3");

        flow.addParagraph()
                .addText()
                .appendText("Alphabetic numberedList is visible only in 'Proof'");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel1)
                .addText()
                .appendText("Alphabetic A");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel1)
                .addText()
                .appendText("Alphabetic B");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel1)
                .addText()
                .appendText("Alphabetic C");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel2)
                .addText()
                .appendText("Alphabetic C.A");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel3)
                .addText()
                .appendText("Alphabetic C.A.A");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel3)
                .addText()
                .appendText("Alphabetic C.A.B");

        flow.addParagraph()
                .setParagraphStyle(alphaParagraphStyleLevel4)
                .addText()
                .appendText("Alphabetic C.A.B.A");
    }

    private void addNumberingRomanList(Layout layout, Flow flow, TextStyle textStyle) {
        Variable level1 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("RomanLevel1");

        Variable level2 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("RomanLevel2");

        Variable level3 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("RomanLevel3");

        NumberedList numberedList = layout.addNumberedList()
                .setListConversion(ROMAN_NUMBER);

        ParagraphStyle paragraphStyleLevel1 = numberedList.addIndentLevel(level1, textStyle, ".")
                .setDefaultTabSize(0.002)
                .setName("RomanParagraphStyleLevel1");

        ParagraphStyle paragraphStyleLevel2 = numberedList.addIndentLevel(level2, textStyle, ".")
                .setFirstLineLeftIndent(0.005)
                .setName("RomanParagraphStyleLevel2");

        ParagraphStyle paragraphStyleLevel3 = numberedList.addIndentLevel(level3, textStyle, ")")
                .setFirstLineLeftIndent(0.010)
                .setDefaultTabSize(0.005)
                .setName("RomanParagraphStyleLevel3");

        flow.addParagraph()
                .setParagraphStyle(paragraphStyleLevel1)
                .addText()
                .appendText("RomanLevel1");
        flow.addParagraph()
                .setParagraphStyle(paragraphStyleLevel2)
                .addText()
                .appendText("RomanLevel2");
        flow.addParagraph()
                .setParagraphStyle(paragraphStyleLevel3)
                .addText()
                .appendText("RomanLevel3");
        flow.addParagraph()
                .setParagraphStyle(paragraphStyleLevel3)
                .addText()
                .appendText("RomanLevel3");
        flow.addParagraph()
                .setParagraphStyle(paragraphStyleLevel3)
                .addText()
                .appendText("RomanLevel3");
    }

    private void addNumberingLevelingList(Layout layout, Flow flow, TextStyle textStyle) {
        Variable level1 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("Level1");

        Variable level2 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("Level2");

        Variable level3 = layout.getData()
                .addVariable()
                .setDataType(INT)
                .setKind(NUMBERING)
                .setName("Level3");

        NumberedList numberedList = layout.addNumberedList();

        ParagraphStyle paragraphStyleLevel1 = numberedList.addIndentLevel(level1, textStyle, ".")
                .setDefaultTabSize(0.002)
                .setName("ParagraphStyleLevel1");
        ParagraphStyle paragraphStyleLevel2 = numberedList.addIndentLevel(level2, textStyle, ".")
                .setFirstLineLeftIndent(0.005)
                .setName("ParagraphStyleLevel2");
        ParagraphStyle paragraphStyleLevel3 = numberedList.addIndentLevel(level3, textStyle, ")")
                .setFirstLineLeftIndent(0.010)
                .setDefaultTabSize(0.005)
                .setName("ParagraphStyleLevel3");

        flow.addParagraph().setParagraphStyle(paragraphStyleLevel1)
                .addText()
                .appendText("ItemLevel1");
        flow.addParagraph().setParagraphStyle(paragraphStyleLevel1)
                .addText()
                .appendText("ItemLevel1");
        flow.addParagraph().setParagraphStyle(paragraphStyleLevel2)
                .addText()
                .appendText("ItemLevel2");
        flow.addParagraph().setParagraphStyle(paragraphStyleLevel3)
                .addText()
                .appendText("ItemLevel3");
        flow.addParagraph().setParagraphStyle(paragraphStyleLevel3)
                .addText()
                .appendText("ItemLevel3");
    }

    private void addNumberingList(Layout layout, Flow flow, TextStyle textStyle) {
        Variable variable = layout.getData()
                .addVariable()
                .setName("Numbering")
                .setKind(NUMBERING)
                .setDataType(INT);

        ParagraphStyle paragraphStyle = layout.addNumberingParagraph(textStyle, ".\t", variable)
                .setNumberingVariable(variable);
        flow.addParagraph().setParagraphStyle(paragraphStyle)
                .addText()
                .appendText("Number1");
        flow.addParagraph().setParagraphStyle(paragraphStyle)
                .addText()
                .appendText("Number2");

        ParagraphStyle resetParagraphStyle = layout.addNumberingParagraph(textStyle, ".\t", variable)
                .setNumberingVariable(variable)
                .setNumberingType(RESET)
                .setNumberingFrom(12);
        flow.addParagraph().setParagraphStyle(resetParagraphStyle)
                .addText()
                .appendText("Reset list and set number 12");
        flow.addParagraph().setParagraphStyle(paragraphStyle)
                .addText()
                .appendText("Number13");
    }

    private void addBulletList(Layout layout, Flow flow) {
        char windingFullCircle = 108;
        char windingsEmptyCircle = 109;
        char windingsFullSquare = 110;

        Font font = layout.addFont().setName(Paths.get(fontPath).getFileName().toString());
        font.addSubfont().setLocation(fontPath, LocationType.DISK);
        TextStyle bulletTextStyle = layout.addTextStyle().setFont(font);

        ParagraphStyle bulletParagraph = layout.addBulletParagraph(
                bulletTextStyle,
                String.format("%c\t", windingFullCircle)
        ).setDefaultTabSize(0.005);
        flow.addParagraph().setParagraphStyle(bulletParagraph)
                .addText()
                .appendText("Bullet");

        ParagraphStyle subBulletParagraph = layout.addBulletParagraph(
                        bulletTextStyle,
                        String.format("%c\t", windingsEmptyCircle)
                ).setFirstLineLeftIndent(0.01)
                .setDefaultTabSize(0.005);
        flow.addParagraph().setParagraphStyle(subBulletParagraph)
                .addText()
                .appendText("SubBullet");

        ParagraphStyle subSubBulletParagraph = layout.addBulletParagraph(
                        bulletTextStyle,
                        String.format("%c\t", windingsFullSquare)
                ).setFirstLineLeftIndent(0.02)
                .setDefaultTabSize(0.005);
        flow.addParagraph().setParagraphStyle(subSubBulletParagraph)
                .addText()
                .appendText("SubSubBullet");
    }
}