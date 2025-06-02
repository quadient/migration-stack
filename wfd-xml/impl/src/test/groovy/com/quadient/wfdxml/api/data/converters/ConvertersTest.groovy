package com.quadient.wfdxml.api.data.converters

import com.quadient.wfdxml.internal.data.converters.BT2BTFCV
import com.quadient.wfdxml.internal.data.converters.ToDateTimeFCV
import spock.lang.Specification

import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.BOOL
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.CURRENCY
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DATETIME
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DOUBLE
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT64
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING

class ConvertersTest extends Specification {
    def "test boolConverter"() {
        when:
        BT2BTFCV c = Converters.boolConverter() as BT2BTFCV

        then:
        c instanceof FieldConverter
        c.inputType == STRING
        c.outputType == BOOL
    }

    def "test intConverter"() {
        when:
        BT2BTFCV c = Converters.intConverter() as BT2BTFCV
        then:
        c instanceof IntFieldConverter
        c.inputType == STRING
        c.outputType == INT
    }

    def "test int64Converter"() {
        when:
        BT2BTFCV c = Converters.int64Converter() as BT2BTFCV
        then:
        c instanceof IntFieldConverter
        c.inputType == STRING
        c.outputType == INT64
    }

    def "test doubleConvertor"() {
        when:
        BT2BTFCV c = Converters.doubleConverter() as BT2BTFCV
        then:
        c instanceof DoubleFieldConverter
        c.inputType == STRING
        c.outputType == DOUBLE
    }

    def "test currencyConvertor"() {
        when:
        BT2BTFCV c = Converters.currencyConverter() as BT2BTFCV
        then:
        c instanceof DoubleFieldConverter
        c.inputType == STRING
        c.outputType == CURRENCY
    }

    def "test dateTimeConvertor"() {
        when:
        ToDateTimeFCV c = Converters.dateTimeConverter() as ToDateTimeFCV
        then:
        c instanceof DateTimeFieldConverter
        c.inputType == STRING
        c.outputType == DATETIME
    }
}