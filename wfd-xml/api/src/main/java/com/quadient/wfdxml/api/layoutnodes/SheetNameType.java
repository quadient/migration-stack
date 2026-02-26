package com.quadient.wfdxml.api.layoutnodes;

public enum SheetNameType {
    PDF_TITLE(37),
    PDF_AUTHOR(38),
    PDF_SUBJECT(39),
    PDF_KEYWORDS(40),
    PDF_PRODUCER(41);

    private final int index;

    SheetNameType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
