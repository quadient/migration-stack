package com.quadient.wfdxml.internal.layoutnodes.data;

public class LAVariableIface {
    public enum VariableType {
        CONSTANT,  //vtConstant=0,
        DATA_VARIABLE, //vtDataVariable=1
        REAL_VARIABLE, //vtRealVariable=2
        CALCULATED, //vtCalculated=3
        NUMBERING, //vtNumbering=4
        DISCONNECTED, //tDisconnected=5
        OVERFLOW_OBJECT, //vtOverflowObject=6
        EXTERNAL_CALCULATED, //vtExternalCalculated=7
        SEARCH_ARRAY_FOR_VALUE, //vtSearchArrayForValue=8
    }
}