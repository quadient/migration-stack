package com.quadient.wfdxml.api.layoutnodes.data;

import com.quadient.wfdxml.api.data.DataDefinition;

public interface Data {

    Variable getSystemVariable(SystemVariables type);

    Data importDataDefinition(DataDefinition dataDefinition);

    Variable addVariable();

    Variable findVariable(String... path);

    Data setRepeatedBy(String id);

    Data setLanguageVariable(Variable variable);
}