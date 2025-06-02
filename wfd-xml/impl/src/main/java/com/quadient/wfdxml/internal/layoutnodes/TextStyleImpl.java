package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.Font;
import com.quadient.wfdxml.api.layoutnodes.LineStyle;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class TextStyleImpl extends NodeImpl<TextStyle> implements TextStyle {

    /**
     * Inspire Designer  #define DPI_TS (72.0*100.0/2.54)
     */
    private static final double DPI_TS = 72.0d * 100.0 / 2.54;
    private final String type = "Simple";
    private final boolean isVisible = true;
    private FillStyle fillStyle;
    private Font font;
    private BorderStyle borderStyle;
    private Variable urlLink;
    private String ancestorId = "Def.TextStyle";
    private String language = "en";
    private double fontSize = 10 / DPI_TS;
    private double lineWidth = 0.0001;
    private double baselineShift = 0;
    private double interCharacterSpacing = 0;
    private double horizontalScale = 1.0d;
    private double fixedWidth = 0.003d;
    private boolean isUnderline = false;
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isStrikeThrough = false;
    private boolean isSmallCap = false;
    private boolean isSuperScript = false;
    private boolean isSubScript = false;
    private boolean connectBorders = false;
    private boolean withLineGab = false;
    private boolean kerning = false;
    private boolean isFixedWidth = false;

    private LineStyle underlineStyleId;
    private LineStyle strikeThroughStyleId;

    public TextStyleImpl() {
    }

    @Override
    public TextStyleImpl seItalic(boolean italic) {
        this.isItalic = italic;
        return this;
    }

    @Override
    public TextStyleImpl setWithLineGap(boolean withLineGap) {
        this.withLineGab = withLineGap;
        return this;
    }

    @Override
    public TextStyleImpl setUnderlineStyle(LineStyle lineStyle) {
        this.underlineStyleId = lineStyle;
        return this;
    }

    @Override
    public TextStyleImpl setStrikeThroughStyle(LineStyle lineStyle) {
        this.strikeThroughStyleId = lineStyle;
        return this;
    }

    @Override
    public TextStyleImpl setInterSpacing(double interSpacing) {
        this.interCharacterSpacing = interSpacing;
        return this;
    }

    @Override
    public TextStyleImpl setUrlTarget(Variable variable) {
        return setUrlLink(variable);
    }

    @Override
    public String getXmlElementName() {
        return "TextStyle";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter
                .addElementWithStringData("AncestorId", ancestorId)
                .addElementWithDoubleData("FontSize", fontSize)
                .addElementWithDoubleData("BaselineShift", baselineShift)
                .addElementWithDoubleData("InterCharacterSpacing", interCharacterSpacing)
                .addElementWithBoolData("Kerning", kerning)
                .addElementWithDoubleData("LineWidth", lineWidth)
                .addElementWithIface("BorderStyleId", borderStyle)
                .addElementWithBoolData("IsVisible", isVisible)
                .addElementWithBoolData("ConnectBorders", connectBorders)
                .addElementWithBoolData("WithLineGap", withLineGab);
        if (fillStyle != null) {
            exporter.addElementWithIface("FillStyleId", fillStyle);
        }
        if (font != null) {
            exporter.addElementWithIface("FontId", font);
        }
        exporter.addElementWithBoolData("Bold", isBold)
                .addElementWithBoolData("Italic", isItalic)
                .addElementWithBoolData("Underline", isUnderline)
                .addElementWithBoolData("Strikethrough", isStrikeThrough)
                .addElementWithStringData("Language", language)
                .addElementWithBoolData("SmallCap", isSmallCap)
                .addElementWithBoolData("SuperScript", isSuperScript)
                .addElementWithBoolData("SubScript", isSubScript)
                .addElementWithIface("URLLink", urlLink)
                .addElementWithDoubleData("HorizontalScale", horizontalScale * 100.0d)
                .addElementWithStringData("Type", type)
                .addElementWithIface("StrikethroughLineStyleId", strikeThroughStyleId)
                .addElementWithIface("UnderlineLineStyleId", underlineStyleId)
                .addElementWithBoolData("IsFixedWidth", isFixedWidth)
                .addElementWithDoubleData("FixedWidth", fixedWidth);
    }

    public FillStyle getFillStyle() {
        return fillStyle;
    }

    @Override
    public TextStyleImpl setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        return this;
    }

    public Font getFont() {
        return font;
    }

    @Override
    public TextStyleImpl setFont(Font font) {
        this.font = font;
        return this;
    }

    public String getAncestorId() {
        return ancestorId;
    }

    public void setAncestorId(String ancestorId) {
        this.ancestorId = ancestorId;
    }

    public String getType() {
        return type;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public TextStyleImpl setLanguage(String language) {
        this.language = language;
        return this;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public double getFontSize() {
        return fontSize;
    }

    @Override
    public TextStyleImpl setFontSize(double size) {
        this.fontSize = size / DPI_TS;
        return this;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    @Override
    public TextStyleImpl setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    public boolean isUnderline() {
        return isUnderline;
    }

    @Override
    public TextStyleImpl setUnderline(boolean underline) {
        this.isUnderline = underline;
        return this;
    }

    public boolean isBold() {
        return isBold;
    }

    @Override
    public TextStyleImpl setBold(boolean bold) {
        this.isBold = bold;
        return this;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public boolean isStrikeThrough() {
        return isStrikeThrough;
    }

    @Override
    public TextStyleImpl setStrikeThrough(boolean strikeThrough) {
        this.isStrikeThrough = strikeThrough;
        return this;
    }

    public boolean isSmallCap() {
        return isSmallCap;
    }

    @Override
    public TextStyleImpl setSmallCap(boolean smallCap) {
        this.isSmallCap = smallCap;
        return this;
    }

    public boolean isSuperScript() {
        return isSuperScript;
    }

    @Override
    public TextStyleImpl setSuperScript(boolean superScript) {
        this.isSuperScript = superScript;
        return this;
    }

    public boolean isSubScript() {
        return isSubScript;
    }

    @Override
    public TextStyleImpl setSubScript(boolean subScript) {
        this.isSubScript = subScript;
        return this;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }

    @Override
    public TextStyleImpl setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle;
        return this;
    }

    public boolean isConnectBorders() {
        return connectBorders;
    }

    @Override
    public TextStyleImpl setConnectBorders(boolean connectBorders) {
        this.connectBorders = connectBorders;
        return this;
    }

    public boolean isWithLineGab() {
        return withLineGab;
    }

    public Variable getUrlLink() {
        return urlLink;
    }

    public TextStyleImpl setUrlLink(Variable urlLink) {
        this.urlLink = urlLink;
        return this;
    }

    public double getBaselineShift() {
        return baselineShift;
    }

    @Override
    public TextStyleImpl setBaselineShift(double baselineShift) {
        this.baselineShift = baselineShift;
        return this;
    }

    public double getInterCharacterSpacing() {
        return interCharacterSpacing;
    }

    public double getHorizontalScale() {
        return horizontalScale;
    }

    @Override
    public TextStyleImpl setHorizontalScale(double horizontalScale) {
        this.horizontalScale = horizontalScale / 100.0d;
        return this;
    }

    public double getFixedWidth() {
        return fixedWidth;
    }

    @Override
    public TextStyleImpl setFixedWidth(double width) {
        this.fixedWidth = width;
        return this;
    }

    public boolean isKerning() {
        return kerning;
    }

    @Override
    public TextStyleImpl setKerning(boolean kerning) {
        this.kerning = kerning;
        return this;
    }

    public boolean isUseFixedWidth() {
        return isFixedWidth;
    }

    @Override
    public TextStyleImpl setUseFixedWidth(boolean useFixedWidth) {
        this.isFixedWidth = useFixedWidth;
        return this;
    }

    public LineStyle getUnderlineStyleId() {
        return underlineStyleId;
    }

    public LineStyle getStrikeThroughStyleId() {
        return strikeThroughStyleId;
    }
}
