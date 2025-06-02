package com.quadient.wfdxml.internal.data.converters;

import com.quadient.wfdxml.api.data.converters.CustCodeFieldConvertor;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustStr2StrFCV extends FieldConverterImpl<CustStr2StrFCV> implements CustCodeFieldConvertor {

    private final List<Case> cases = new ArrayList<>();
    private String defResult = "Default";
    private boolean passThrough = false;

    public String getDefResult() {
        return defResult;
    }

    public CustStr2StrFCV setDefResult(String defResult) {
        this.defResult = defResult;
        return this;
    }

    @Override
    public CustStr2StrFCV setDefaultValue(String defaultValue) {
        return setDefResult(defaultValue);
    }

    public boolean isPassThrough() {
        return passThrough;
    }

    @Override
    public CustStr2StrFCV setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
        return this;
    }

    public List<Case> getCases() {
        return Collections.unmodifiableList(cases);
    }

    @Override
    public CustStr2StrFCV addCase(String inputString, String outputString) {
        cases.add(new Case(inputString, outputString));
        return this;
    }

    @Override
    String getFieldConverterClassNameForExport() {
        return "CustStr2StrFCV";
    }

    @Override
    void exportFieldConverterSettings(XmlExporter exporter) {
        exporter.addElementWithStringData("DefaultValue", defResult);
        exporter.addElementWithBoolData("PassThrough", passThrough);
        for (Case aCase : cases) {
            exporter.beginElement("Field");
            exporter.addStringAttribute("Key", aCase.inputString);
            exporter.addStringAttribute("Value", aCase.outputString);
            exporter.endElement();
        }
    }

    private class Case {
        public String inputString = "";
        public String outputString = "";

        public Case(String inputString, String outputString) {
            this.inputString = inputString;
            this.outputString = outputString;
        }
    }
}

