package com.quadient.wfdxml.api.data.converters;

import com.quadient.wfdxml.internal.data.converters.BT2BTFCV;
import com.quadient.wfdxml.internal.data.converters.CustStr2StrFCV;
import com.quadient.wfdxml.internal.data.converters.ToDateTimeFCV;

import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.BOOL;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.CURRENCY;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DOUBLE;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT64;

public class Converters {
    private Converters() {
    }

    public static FieldConverter boolConverter() {
        return new BT2BTFCV().setOutputType(BOOL);
    }

    public static IntFieldConverter intConverter() {
        return new BT2BTFCV().setOutputType(INT);
    }

    public static IntFieldConverter int64Converter() {
        return new BT2BTFCV().setOutputType(INT64);
    }

    public static DoubleFieldConverter doubleConverter() {
        return new BT2BTFCV().setOutputType(DOUBLE);
    }

    public static DoubleFieldConverter currencyConverter() {
        return new BT2BTFCV().setOutputType(CURRENCY);
    }

    public static DateTimeFieldConverter dateTimeConverter() {
        return new ToDateTimeFCV();
    }

    public static CustCodeFieldConvertor custCodeConverter() {
        return new CustStr2StrFCV();
    }
}