package com.quadient.wfdxml.api.layoutnodes;

public enum SheetNameType {
    PDF_TITLE(37),
    PDF_AUTHOR(38),
    PDF_SUBJECT(39),
    PDF_KEYWORDS(40),
    PDF_PRODUCER(41),

    EMAIL_FROM_NAME(47),
    EMAIL_FROM(48),

    EMAIL_TO(50),

    EMAIL_SUBJECT(57),

    SMS_NUMBER_TO(79);

    private final int index;

    SheetNameType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
