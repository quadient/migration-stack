package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface NumberedList {

    ParagraphStyle addIndentLevel(Variable variable, TextStyle textStyle, String suffix);

    NumberedList setTabAfterLastNumber(boolean tabAfterLastNumber);

    NumberedList setSuffixAfterLastNumber(boolean noneAfterLastNumber);

    NumberedList setListConversion(ListConversionType listConversionType);

    enum ListConversionType {
        /**
         * I II III IV V ...
         */
        ROMAN_NUMBER,
        /**
         * A B C ... Y Z AA BB CC
         */
        ALPHABETIC,
        /**
         * A B C ... Y Z A B C
         */
        REPEATED_ALPHABETIC
    }
}
