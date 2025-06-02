package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;

public interface TextStyle extends Node<TextStyle> {

    TextStyle setFillStyle(FillStyle fillStyle);

    TextStyle setFont(Font font);

    TextStyle setFontSize(double sizeInPoints);

    TextStyle setLineWidth(double width);

    TextStyle setBold(boolean bold);

    TextStyle seItalic(boolean italic);

    TextStyle setUnderline(boolean underline);

    TextStyle setStrikeThrough(boolean strikeThrough);

    TextStyle setSmallCap(boolean smallCap);

    TextStyle setSuperScript(boolean superScript);

    TextStyle setSubScript(boolean subScript);

    TextStyle setBaselineShift(double baselineShift);

    TextStyle setInterSpacing(double interSpacing);

    TextStyle setKerning(boolean kerning);

    TextStyle setUseFixedWidth(boolean useFixedWidth);

    TextStyle setFixedWidth(double width);

    TextStyle setLanguage(String language);

    /**
     * in percent
     */
    TextStyle setHorizontalScale(double horizontalScale);

    TextStyle setUrlTarget(Variable variable);

    TextStyle setUnderlineStyle(LineStyle lineStyle);

    TextStyle setStrikeThroughStyle(LineStyle lineStyle);

    TextStyle setBorderStyle(BorderStyle borderStyle);

    TextStyle setConnectBorders(boolean connectBorders);

    TextStyle setWithLineGap(boolean withLineGap);
}
