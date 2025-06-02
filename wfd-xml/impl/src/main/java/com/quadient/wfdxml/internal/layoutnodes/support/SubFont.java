package com.quadient.wfdxml.internal.layoutnodes.support;

public class SubFont {
    private String fontLocation = "FONT_DIR,Arial.TTF";
    private int fontIndex = 0;

    private boolean bold = false;
    private boolean italic = false;

    public SubFont() {
    }

    public SubFont setSubFont(String font) {
        this.fontLocation = "FONT_DIR," + font + ".TTF";
        return this;
    }

    public SubFont setSubFontDiskLocation(String fontPath) {
        this.fontLocation = "DiskLocation," + fontPath;
        return this;
    }

    public int getFontIndex() {
        return fontIndex;
    }

    public SubFont setFontIndex(int fontIndex) {
        this.fontIndex = fontIndex;
        return this;
    }

    public String getFontLocation() {
        return fontLocation;
    }

    public SubFont setFontLocation(String fontLocation) {
        this.fontLocation = fontLocation;
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public SubFont setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public boolean isBold() {
        return bold;
    }

    public SubFont setBold(boolean bold) {
        this.bold = bold;
        return this;
    }
}
