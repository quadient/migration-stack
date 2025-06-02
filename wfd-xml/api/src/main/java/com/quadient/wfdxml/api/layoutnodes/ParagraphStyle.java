package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;

public interface ParagraphStyle extends Node<ParagraphStyle> {

    ParagraphStyle setTextStyle(TextStyle textStyle);

    ParagraphStyle setFirstLineLeftIndent(double firstLineLeftIndent);

    ParagraphStyle setSpaceBefore(double spaceBefore);

    ParagraphStyle setSpaceAfter(double spaceAfter);

    ParagraphStyle setLeftIndent(double indent);

    ParagraphStyle setRightIndent(double indent);

    ParagraphStyle setAlignType(AlignType type);

    ParagraphStyle setIndent(double before, double right, double after, double left);

    ParagraphStyle setLineSpacingValue(double lineSpacing);

    ParagraphStyle setLineSpacingType(LineSpacingType type);

    ParagraphStyle setSpaceBeforeFirstPara(boolean space);

    ParagraphStyle setTakeMaxFromSpace(boolean space);

    ParagraphStyle setDistributeLineSpacing(boolean spacing);

    ParagraphStyle setIgnoreEmptyLines(boolean ignoreEmptyLines);

    ParagraphStyle setBorderStyle(BorderStyle borderStyle);

    ParagraphStyle setConnectBorders(boolean connectBorders);

    ParagraphStyle setWithLineGap(boolean withLineGap);

    ParagraphStyle setDefaultTabSize(double size);

    ParagraphStyle setUseOutsideTabs(boolean outsideTabs);

    ParagraphStyle addTabulator(double pos, TabulatorType type);

    ParagraphStyle addDecimalTabulatorWithPoint(double pos, String point);

    ParagraphStyle setBulletsNumberingFlow(Flow bulletsFlow);

    ParagraphStyle setNumberingVariable(Variable numberingVariable);

    ParagraphStyle setNumberingType(NumberingType type);

    ParagraphStyle setNumberingFrom(int numberingFrom);

    ParagraphStyle setHyphenate(boolean hyphenate);

    enum AlignType {
        LEFT,
        RIGHT,
        CENTER,
        JUSTIFY_lEFT,
        JUSTIFY_RIGHT,
        JUSTIFY_CENTER,
        JUSTIFY_BLOCK,
        JUSTIFY_WITH_MARGIN,
        JUSTIFY_BLOCK_UNIFORM
    }

    enum LineSpacingType {
        ADDITIONAL,
        EXACT,
        AT_LEAST,
        MULTIPLE_OF,
        EXACT_FROM_PREVIOUS_WITH_ADJUST_OLD,
        EXACT_FROM_PREVIOUS,
        EXACT_FROM_PREVIOUS_WITH_ADJUST

    }

    enum NumberingType {
        INCREMENT,
        RESET,
        NONE
    }
}