package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.NumberedList
import com.quadient.wfdxml.api.layoutnodes.TextStyle
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.NumberedList.ListConversionType.ALPHABETIC
import static com.quadient.wfdxml.api.layoutnodes.NumberedList.ListConversionType.REPEATED_ALPHABETIC
import static com.quadient.wfdxml.api.layoutnodes.NumberedList.ListConversionType.ROMAN_NUMBER
import static com.quadient.wfdxml.api.layoutnodes.data.DataType.INT
import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.ARABIC_TO_ROMAN_NUMBER
import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.SCRIPT
import static com.quadient.wfdxml.api.layoutnodes.data.VariableKind.NUMBERING

class NumberedListImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "addVariableLevel add Variable into flow"() {
        given:
        Variable variableMock = Mock(VariableImpl.class)
        TextStyle textStyleMock = Mock(TextStyleImpl.class)
        exporter.idRegister.getOrCreateId(variableMock)
        LayoutImpl layout = new LayoutImpl()

        when:
        NumberedListImpl numberedList = layout.addNumberedList()
        numberedList.addIndentLevel(variableMock, textStyleMock, ":")

        then:
        layout.export(exporter)
        String result = exporter.buildString()
        assert result.count('<O Id="SR_1">') == 1
    }

    def "addIndentLevel with 3 levels must add six Variables int flow"() {
        given:
        Variable variableMock1 = Mock(VariableImpl.class)
        Variable variableMock2 = Mock(VariableImpl.class)
        Variable variableMock3 = Mock(VariableImpl.class)
        TextStyle textStyleMock = Mock(TextStyleImpl.class)
        exporter.idRegister.getOrCreateId(variableMock1)
        exporter.idRegister.getOrCreateId(variableMock2)
        exporter.idRegister.getOrCreateId(variableMock3)
        LayoutImpl layout = new LayoutImpl()

        when:
        NumberedListImpl numberedList = layout.addNumberedList()
        numberedList.addIndentLevel(variableMock1, textStyleMock, ".")
        numberedList.addIndentLevel(variableMock2, textStyleMock, ".")
        numberedList.addIndentLevel(variableMock3, textStyleMock, ".")

        then:
        layout.export(exporter)
        String result = exporter.buildString()
        assert result.count('<O Id="SR_1"></O>') == 3
        assert result.count('<O Id="SR_2"></O>') == 2
        assert result.count('<O Id="SR_3"></O>') == 1
        assert result.contains('<O Id="SR_1"></O>.')
        assert result.contains('<O Id="SR_1"></O>.<O Id="SR_2"></O>.')
        assert result.contains('<O Id="SR_1"></O>.<O Id="SR_2"></O>.<O Id="SR_3"></O>.')
    }

    def "NumberedList with setSuffixAfterLastNumber set false"() {
        given:
        Variable variableMock1 = Mock(VariableImpl.class)
        Variable variableMock2 = Mock(VariableImpl.class)
        TextStyle textStyleMock = Mock(TextStyleImpl.class)
        exporter.idRegister.getOrCreateId(variableMock1)
        exporter.idRegister.getOrCreateId(variableMock2)
        LayoutImpl layout = new LayoutImpl()
        NumberedList numberedList = layout.addNumberedList() as NumberedList

        when:
        numberedList.setSuffixAfterLastNumber(false)
        numberedList.addIndentLevel(variableMock1, textStyleMock, ".")
        numberedList.addIndentLevel(variableMock2, textStyleMock, ".")

        then:
        layout.export(exporter)
        String result = exporter.buildString()

        assertThereIsNoSuffixDotInTheEndForFirstIndentLevel(result)
        assertThereIsNoSuffixDotInTheEndForSecondIndentLevel(result)
    }

    private void assertThereIsNoSuffixDotInTheEndForSecondIndentLevel(String result) {
        assert result.contains('<O Id="SR_1"></O>.<O Id="SR_2"></O><Tab></Tab>')
    }

    private void assertThereIsNoSuffixDotInTheEndForFirstIndentLevel(String result) {
        assert result.contains('<O Id="SR_1"></O><Tab></Tab>')
    }

    def "addVariableLevel add suffix after Variable "() {
        given:
        Variable variableMock = Mock(VariableImpl.class)
        TextStyle textStyleMock = Mock(TextStyleImpl.class)
        exporter.idRegister.getOrCreateId(variableMock)

        LayoutImpl layout = new LayoutImpl()
        NumberedListImpl numberedList = layout.addNumberedList()
        numberedList.addIndentLevel(variableMock, textStyleMock, "QQQ")

        when:
        layout.export(exporter)

        then:
        String result = exporter.buildString()
        assert result.count("QQQ") == 1
        assert result.contains('<O Id="SR_1"></O>QQQ<Tab></Tab>')
    }

    def "not using auto tab with setTabAfterLastNumber"() {
        given:
        Variable variableMock = Mock(VariableImpl.class)
        TextStyle textStyleMock = Mock(TextStyleImpl.class)

        LayoutImpl layout = new LayoutImpl()
        NumberedListImpl numberedList = layout.addNumberedList().setTabAfterLastNumber(false)
        numberedList.addIndentLevel(variableMock, textStyleMock, ".")

        when:
        layout.export(exporter)

        then:
        String result = exporter.buildString()
        assert !result.contains("<Tab></Tab>")
    }

    def "addIndentLevel for roman numberedList set correctly variable conversion\""() {
        given:
        TextStyle textStyleMock = Mock(TextStyleImpl.class)

        LayoutImpl layout = new LayoutImpl()
        Variable variable = layout.getData().addVariable().setKind(NUMBERING).setDataType(INT)
        NumberedListImpl numberedList = layout.addNumberedList().setListConversion(ROMAN_NUMBER)

        when:
        numberedList.addIndentLevel(variable, textStyleMock, ".")

        then:
        variable.fcvClassName == ARABIC_TO_ROMAN_NUMBER
    }

    def "addIndentLevel for repeatedAlphabetic numberedList set correctly variable conversion"() {
        given:
        TextStyle textStyleMock = Mock(TextStyleImpl.class)

        LayoutImpl layout = new LayoutImpl()
        VariableImpl variable = layout.getData().addVariable().setKind(NUMBERING).setDataType(INT)
        NumberedListImpl numberedList = layout.addNumberedList().setListConversion(REPEATED_ALPHABETIC)

        when:
        numberedList.addIndentLevel(variable, textStyleMock, ".")

        then:
        variable.fcvClassName == SCRIPT
        variable.expression == "return Char(65 + ((Input - 1) % 26));"
    }

    def "addIndentLevel for alphabetic with numberedList set correctly variable conversion "() {
        given:
        TextStyle textStyleMock = Mock(TextStyleImpl.class)
        LayoutImpl layout = new LayoutImpl()
        Variable variable = layout.getData().addVariable().setKind(NUMBERING).setDataType(INT)
        NumberedListImpl numberedList = layout.addNumberedList().setListConversion(ALPHABETIC)

        when:
        numberedList.addIndentLevel(variable, textStyleMock, ".")

        then:
        variable.fcvClassName == SCRIPT
        variable.expression == "String x;\r\n" +
                "for (Int i=0; i < (Input/27)+1; i++) {\r\n" +
                "x+= Char(65+(Input - 1 % 26));\r\n" +
                "}\r\n" +
                "return x;"
    }
}