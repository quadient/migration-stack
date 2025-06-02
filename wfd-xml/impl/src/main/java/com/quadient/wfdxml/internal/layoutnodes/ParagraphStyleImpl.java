package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle;
import com.quadient.wfdxml.api.layoutnodes.TabulatorType;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.layoutnodes.support.TabulatorProperties;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.NumberingType.INCREMENT;
import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.NumberingType.RESET;

public class ParagraphStyleImpl extends NodeImpl<ParagraphStyle> implements ParagraphStyle {

    private final TabulatorProperties tabulatorProperties = new TabulatorProperties();
    private TextStyle textStyle;
    private Flow bulletsNumberingFlow;
    private Variable numberingVariableId;
    private BorderStyle borderStyle;
    private String ancestorId = "Def.ParaStyle";
    private String type = "Simple";
    private LineSpacingType lineSpacingType = LineSpacingType.ADDITIONAL;
    private AlignType alignType = AlignType.LEFT;
    private NumberingType numberingType = INCREMENT;

    private boolean visible = true;
    private boolean spaceBeforeFirstPara = false;
    private boolean calcMaxSpaceBeforeAfter = false;
    private boolean distributeLineSpacing = false;
    private boolean ignoreEmptyLines = false;
    private boolean connectBorders = false;
    private boolean withLineGab = false;

    private boolean hyphenate = false;

    private int numberingFrom = 1;

    private double leftIndent = 0;
    private double rightIndent = 0;
    private double firstLineLeftIndent = 0;
    private double spaceBefore = 0;
    private double spaceAfter = 0;
    private double lineSpacingValue = 0;

    public ParagraphStyleImpl() {
    }

    public static String convertNumberingTypeToXmlName(NumberingType numberingType) {
        switch (numberingType) {
            case INCREMENT:
                return "Increment";
            case RESET:
                return "Reset";
            case NONE:
                return "None";
            default:
                throw new IllegalStateException(numberingType.toString());
        }
    }

    public static String convertAlignTypeToXmlName(AlignType alignType) {
        switch (alignType) {
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            case CENTER:
                return "Center";
            case JUSTIFY_lEFT:
                return "JustifyLeft";
            case JUSTIFY_RIGHT:
                return "JustifyRight";
            case JUSTIFY_CENTER:
                return "JustifyCenter";
            case JUSTIFY_BLOCK:
                return "JustifyBlock";
            case JUSTIFY_WITH_MARGIN:
                return "JustifyBlock2";
            case JUSTIFY_BLOCK_UNIFORM:
                return "JustifyBlockUniform";
            default:
                throw new IllegalStateException(alignType.toString());
        }
    }

    public static String convertLineSpacingTypeToXmlName(LineSpacingType lineSpacingType) {
        switch (lineSpacingType) {
            case ADDITIONAL:
                return "Additional";
            case EXACT:
                return "Exact";
            case AT_LEAST:
                return "AtLeast";
            case MULTIPLE_OF:
                return "MultipleOf";
            case EXACT_FROM_PREVIOUS_WITH_ADJUST_OLD:
                return "ExactFromPrevWithAdj";
            case EXACT_FROM_PREVIOUS:
                return "ExactFromPrev";
            case EXACT_FROM_PREVIOUS_WITH_ADJUST:
                return "ExactFromPrevWithAdj2";

            default:
                throw new IllegalStateException(lineSpacingType.toString());
        }
    }

    @Override
    public ParagraphStyleImpl setIndent(double top, double right, double bottom, double left) {
        this.spaceBefore = top;
        this.rightIndent = right;
        this.spaceAfter = bottom;
        this.leftIndent = left;
        return this;
    }

    @Override
    public ParagraphStyleImpl setTakeMaxFromSpace(boolean space) {
        this.calcMaxSpaceBeforeAfter = space;
        return this;
    }

    @Override
    public ParagraphStyleImpl setDefaultTabSize(double space) {
        tabulatorProperties.setDefaultTab(space);
        return this;
    }

    @Override
    public ParagraphStyleImpl setUseOutsideTabs(boolean outsideTabs) {
        tabulatorProperties.setUseOutsideTabs(outsideTabs);
        return this;
    }

    @Override
    public ParagraphStyleImpl addTabulator(double pos, TabulatorType type) {
        tabulatorProperties.addTabulator(pos, type);
        return this;
    }

    @Override
    public ParagraphStyleImpl addDecimalTabulatorWithPoint(double pos, String point) {
        tabulatorProperties.addDecimalTabulatorWithPoint(pos, point);
        return this;
    }

    @Override
    public ParagraphStyleImpl setNumberingVariable(Variable numberingVariable) {
        this.numberingVariableId = numberingVariable;
        return this;
    }

    @Override
    public ParagraphStyleImpl setWithLineGap(boolean withLineGap) {
        this.withLineGab = withLineGap;
        return this;
    }

    @Override
    public ParagraphStyleImpl setHyphenate(boolean hyphenate) {
        this.hyphenate = hyphenate;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "ParaStyle";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter
                .addElementWithStringData("AncestorId", ancestorId)
                .addElementWithDoubleData("LeftIndent", leftIndent)
                .addElementWithDoubleData("RightIndent", rightIndent)
                .addElementWithDoubleData("SpaceBefore", spaceBefore)
                .addElementWithDoubleData("SpaceAfter", spaceAfter)
                .addElementWithDoubleData("FirstLineLeftIndent", firstLineLeftIndent + leftIndent)
                .addElementWithStringData("HAlign", convertAlignTypeToXmlName(alignType))
                .addElementWithIface("BorderStyleId", borderStyle)
                .addElementWithBoolData("IsVisible", visible)
                .addElementWithBoolData("ConnectBorders", connectBorders)
                .addElementWithBoolData("WithLineGap", withLineGab)
                .addElementWithIface("BullettingId", bulletsNumberingFlow)
                .addElementWithBoolData("IgnoreEmptyLines", ignoreEmptyLines);

        tabulatorProperties.export(exporter);

        exporter.beginElement("Hyphenation");
        exporter.addElementWithBoolData("Hyphenate", hyphenate);
        exporter.endElement();

        exporter.addElementWithStringData("NumberingType", convertNumberingTypeToXmlName(numberingType))
                .addElementWithIface("NumberingVariableId", numberingVariableId);

        if (numberingType == RESET) {
            exporter.addElementWithIntData("NumberingFrom", numberingFrom);
        }

        exporter.addElementWithBoolData("CalcMaxSpaceBeforeAfter", calcMaxSpaceBeforeAfter)
                .addElementWithBoolData("DistributeLineSpace", distributeLineSpacing);
        if (textStyle != null) {
            exporter.addElementWithIface("DefaultTextStyleId", textStyle);
        }

        exporter.addElementWithBoolData("SpaceBeforeFirst", spaceBeforeFirstPara)
                .addElementWithDoubleData("LineSpacing", lineSpacingValue)
                .addElementWithStringData("LineSpacingType", convertLineSpacingTypeToXmlName(lineSpacingType))
                .addElementWithStringData("Type", type);
    }

    public String getAncestorId() {
        return ancestorId;
    }

    public ParagraphStyleImpl setAncestorId(String ancestorId) {
        this.ancestorId = ancestorId;
        return this;
    }

    public String getType() {
        return type;
    }

    public ParagraphStyleImpl setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public ParagraphStyleImpl setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    @Override
    public ParagraphStyleImpl setTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle;
        return this;
    }

    public double getLeftIndent() {
        return leftIndent;
    }

    @Override
    public ParagraphStyleImpl setLeftIndent(double leftIndent) {
        this.leftIndent = leftIndent;
        return this;
    }

    public double getRightIndent() {
        return rightIndent;
    }

    @Override
    public ParagraphStyleImpl setRightIndent(double rightIndent) {
        this.rightIndent = rightIndent;
        return this;
    }

    public double getFirstLineLeftIndent() {
        return firstLineLeftIndent;
    }

    @Override
    public ParagraphStyleImpl setFirstLineLeftIndent(double firstLineLeftIndent) {
        this.firstLineLeftIndent = firstLineLeftIndent;
        return this;
    }

    public double getSpaceBefore() {
        return spaceBefore;
    }

    @Override
    public ParagraphStyleImpl setSpaceBefore(double spaceBefore) {
        this.spaceBefore = spaceBefore;
        return this;
    }

    public double getSpaceAfter() {
        return spaceAfter;
    }

    @Override
    public ParagraphStyleImpl setSpaceAfter(double spaceAfter) {
        this.spaceAfter = spaceAfter;
        return this;
    }

    public LineSpacingType getLineSpacingType() {
        return lineSpacingType;
    }

    @Override
    public ParagraphStyleImpl setLineSpacingType(LineSpacingType type) {
        this.lineSpacingType = type;
        return this;
    }

    public AlignType getAlignType() {
        return alignType;
    }

    @Override
    public ParagraphStyleImpl setAlignType(AlignType alignType) {
        this.alignType = alignType;
        return this;
    }

    public boolean isSpaceBeforeFirstPara() {
        return spaceBeforeFirstPara;
    }

    @Override
    public ParagraphStyleImpl setSpaceBeforeFirstPara(boolean space) {
        this.spaceBeforeFirstPara = space;
        return this;
    }

    public boolean isCalcMaxSpaceBeforeAfter() {
        return calcMaxSpaceBeforeAfter;
    }

    public boolean isDistributeLineSpacing() {
        return distributeLineSpacing;
    }

    @Override
    public ParagraphStyleImpl setDistributeLineSpacing(boolean spacing) {
        this.distributeLineSpacing = spacing;
        return this;
    }

    public double getLineSpacingValue() {
        return lineSpacingValue;
    }

    @Override
    public ParagraphStyleImpl setLineSpacingValue(double lineSpacing) {
        this.lineSpacingValue = lineSpacing;
        return this;
    }

    public Flow getBulletsNumberingFlow() {
        return bulletsNumberingFlow;
    }

    @Override
    public ParagraphStyleImpl setBulletsNumberingFlow(Flow bulletsFlow) {
        this.bulletsNumberingFlow = bulletsFlow;
        return this;
    }

    public Variable getNumberingVariableId() {
        return numberingVariableId;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }

    @Override
    public ParagraphStyleImpl setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle;
        return this;
    }

    public TabulatorProperties getTabulatorProperties() {
        return tabulatorProperties;
    }

    public NumberingType getNumberingType() {
        return numberingType;
    }

    @Override
    public ParagraphStyleImpl setNumberingType(NumberingType type) {
        this.numberingType = type;
        return this;
    }

    public boolean isIgnoreEmptyLines() {
        return ignoreEmptyLines;
    }

    @Override
    public ParagraphStyleImpl setIgnoreEmptyLines(boolean ignoreEmptyLines) {
        this.ignoreEmptyLines = ignoreEmptyLines;
        return this;
    }

    public int getNumberingFrom() {
        return numberingFrom;
    }

    @Override
    public ParagraphStyleImpl setNumberingFrom(int numberingFrom) {
        this.numberingFrom = numberingFrom;
        return this;
    }

    public boolean isConnectBorders() {
        return connectBorders;
    }

    @Override
    public ParagraphStyleImpl setConnectBorders(boolean connectBorders) {
        this.connectBorders = connectBorders;
        return this;
    }

    public boolean isWithLineGab() {
        return withLineGab;
    }
}