package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.NumberedList;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.flow.Text;
import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.ARABIC_TO_ROMAN_NUMBER;
import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.SCRIPT;

public class NumberedListImpl implements NumberedList {
    private final List<VariableImpl> variableList = new ArrayList<>();
    private final List<String> suffixList = new ArrayList<>();
    private final Layout layout;

    private boolean tabAfterLastNumber = true;
    private ListConversionType listConversionType;
    private boolean suffixAfterLastNumber = true;

    public NumberedListImpl(Layout layout) {
        this.layout = layout;
    }

    public static String getAlphabeticScript() {
        return "String x;\r\n" +
                "for (Int i=0; i < (Input/27)+1; i++) {\r\n" +
                "x+= Char(65+(Input - 1 % 26));\r\n" +
                "}\r\n" +
                "return x;";
    }

    public static String getAlphabeticRepeatedScript() {
        return "return Char(65 + ((Input - 1) % 26));";
    }

    @Override
    public ParagraphStyleImpl addIndentLevel(Variable variable, TextStyle textStyle, String suffix) {
        if (listConversionType != null) {
            switch (listConversionType) {
                case ROMAN_NUMBER:
                    variable.setConversion(ARABIC_TO_ROMAN_NUMBER);
                    break;
                case REPEATED_ALPHABETIC:
                    variable.setConversion(SCRIPT).setScript(getAlphabeticRepeatedScript());
                    break;
                case ALPHABETIC:
                    variable.setConversion(SCRIPT).setScript(getAlphabeticScript());
                    break;
                default:
                    throw new UnsupportedOperationException("This conversion is not supported yet" + listConversionType);
            }
        }

        variableList.add((VariableImpl) variable);
        suffixList.add(suffix);

        Flow flow = layout.addFlow();
        Text text = flow.addParagraph().addText().setTextStyle(textStyle);

        for (int i = 0; i < variableList.size(); i++) {
            text.appendVariable(variableList.get(i));

            if (i == suffixList.size() - 1) {
                if (suffixAfterLastNumber) {
                    text.appendText(suffixList.get(i));
                }
                if (tabAfterLastNumber) {
                    text.appendText("\t");
                }
            } else {
                text.appendText(suffixList.get(i));
            }

        }

        return (ParagraphStyleImpl) layout.addParagraphStyle().setNumberingVariable(variable).setBulletsNumberingFlow(flow);
    }

    @Override
    public NumberedListImpl setTabAfterLastNumber(boolean tabAfterLastNumber) {
        this.tabAfterLastNumber = tabAfterLastNumber;
        return this;
    }

    @Override
    public NumberedListImpl setListConversion(ListConversionType listConversionType) {
        this.listConversionType = listConversionType;
        return this;
    }

    @Override
    public NumberedListImpl setSuffixAfterLastNumber(boolean noneAfterLastNumber) {
        this.suffixAfterLastNumber = noneAfterLastNumber;
        return this;
    }
}