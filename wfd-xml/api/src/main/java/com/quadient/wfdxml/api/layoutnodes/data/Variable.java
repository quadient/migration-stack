package com.quadient.wfdxml.api.layoutnodes.data;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.FlowArea;

public interface Variable extends Node<Variable> {

    Variable setValue(String value);

    Variable setValue(int value);

    Variable setValue(double value);

    Variable setValue(boolean value);

    Variable setValue(long value);

    Variable setKind(VariableKind kind);

    Variable setDataType(DataType type);

    Variable setScript(String script);

    Variable setFlowArea(FlowArea flow);

    Variable setConversion(ConversionType conversion);

    String getExistingParentId();

    Variable setExistingParentId(String existingParentId);

    Variable setDisplacement(Displacement displacement);

    enum ConversionType {
        SCRIPT,
        ARABIC_TO_ROMAN_NUMBER,
    }
}