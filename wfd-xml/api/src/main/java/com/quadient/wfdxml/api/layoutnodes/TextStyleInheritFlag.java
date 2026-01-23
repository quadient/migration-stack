package com.quadient.wfdxml.api.layoutnodes;

public enum TextStyleInheritFlag {
    FONT("Font"),
    FONT_SIZE("FontSize"),
    BASELINE_SHIFT("BaselineShift"),
    INTER_CHARACTER_SPACING("InterCharacterSpacing"),
    BOLD("Bold"),
    ITALIC("Italic"),
    UNDERLINE("Underline"),
    STRIKETHROUGH("Strikethrough"),
    KERNING("Kerning"),
    LINE_WIDTH("LineWidth"),
    MITER_LIMIT("MiterLimit"),
    CAP_TYPE("CapType"),
    JOIN_TYPE("JoinType"),
    FILL_STYLE("FillStyle"),
    OUTLINE_STYLE("OutlineStyle"),
    BORDER_STYLE("BorderStyle"),
    CONNECT_BORDERS("ConnectBorders"),
    WITH_LINE_GAP("WithLineGap"),
    LANGUAGE("Language"),
    SMALL_CAP("SmallCap"),
    SUPER_SUB_SCRIPT("SuperSubScript"),
    SUPER_SUB_SCRIPT_PROP("SuperSubScriptProp"),
    CUSTOM_UNDER_STRIKE("CustomUnderStrike"),
    URL_LINK("URLLink"),
    URL_BASE_TEXT_STYLE("URLBaseTextStyle"),
    URL_POST_CLICK_FILL_STYLE("URLPostClickFillStyle"),
    HORIZONTAL_SCALE("HorizontalScale"),
    WRAPPING_RULE("WrappingRule"),
    SUB_FONT("SubFont"),
    STRIKETHROUGH_LINE_STYLE("StrikethroughLineStyle"),
    UNDERLINE_LINE_STYLE("UnderlineLineStyle"),
    CSS_FILE("CssFile"),
    URL_ALTERNATE_TEXT("URLAlternateText");

    private final String xmlElementName;

    TextStyleInheritFlag(String xmlElementName) {
        this.xmlElementName = xmlElementName;
    }

    public String getXmlElementName() {
        return xmlElementName;
    }
}
