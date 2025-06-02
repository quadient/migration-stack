package com.quadient.wfdxml.internal.layoutnodes.data;

import com.quadient.wfdxml.api.layoutnodes.FlowArea;
import com.quadient.wfdxml.api.layoutnodes.data.DataType;
import com.quadient.wfdxml.api.layoutnodes.data.Displacement;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.layoutnodes.FlowAreaImpl;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.api.layoutnodes.data.Variable.ConversionType.SCRIPT;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.CALCULATED;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.DATA_VARIABLE;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.EXTERNAL_CALCULATED;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.OVERFLOW_OBJECT;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.SEARCH_ARRAY_FOR_VALUE;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.ARRAY;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.OPTIONAL;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE;

public class VariableImpl extends Tree<Variable> implements Variable {

    private int insideFnc = 0;
    private VariableType type = DATA_VARIABLE;
    private NodeOptionality nodeOptionality = MUST_EXIST;
    private NodeType nodeType = STRING;
    private Displacement displacement = null;

    private FlowAreaImpl flowArea;

    private String constantText;
    private int constantInteger;
    private boolean constantBool;
    private double constantDouble;
    private long constantInt64;
    private String expression = "";
    private ConversionType fcvClassName;

    private String existingParentId = null;

    public static String convertVariableTypeToXmlName(VariableType type) {
        switch (type) {
            case CONSTANT:
                return "Constant";
            case DATA_VARIABLE:
                return "DataVariable";
            case REAL_VARIABLE:
                return "RealVariable";
            case CALCULATED:
                return "Calculated";
            case NUMBERING:
                return "Numbering";
            case DISCONNECTED:
                return "Disconnected";
            case OVERFLOW_OBJECT:
                return "OverflowObject";
            case EXTERNAL_CALCULATED:
                return "ExternalCalculated";
            case SEARCH_ARRAY_FOR_VALUE:
                return "SearchArrayForValue";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    public static String convertNodeTypeToXmlName(NodeType nodeType, NodeOptionality nodeOptionality) {
        switch (nodeType) {
            case STRING:
                return "String";
            case INT:
                return "Int";
            case INT64:
                return "Int64";
            case CURRENCY:
                return "Currency";
            case DOUBLE:
                return "Double";
            case DATETIME:
                return "DateTime";
            case BOOL:
                return "Bool";
            case BINARY:
                return "Binary";
            case SUB_TREE:
                switch (nodeOptionality) {
                    case MUST_EXIST:
                        return "SubTree";
                    case OPTIONAL:
                        return "Optional";
                    case ARRAY:
                        return "Array";
                    default:
                        throw new IllegalStateException(nodeOptionality.toString());
                }
            default:
                throw new IllegalStateException(nodeType.toString());
        }
    }

    public static String convertConversionTypeToXmlName(ConversionType conversionType) {
        switch (conversionType) {
            case SCRIPT:
                return "ScriptFCV";
            case ARABIC_TO_ROMAN_NUMBER:
                return "LatinToRomeNumberFCV";
            default:
                throw new IllegalStateException(conversionType.toString());
        }
    }

    public int getInsideFnc() {
        return insideFnc;
    }

    public VariableImpl setInsideFnc(int insideFnc) {
        this.insideFnc = insideFnc;
        return this;
    }

    public VariableType getType() {
        return type;
    }

    public VariableImpl setType(VariableType type) {
        this.type = type;
        return this;
    }

    public NodeOptionality getNodeOptionality() {
        return nodeOptionality;
    }

    public VariableImpl setNodeOptionality(NodeOptionality nodeOptionality) {
        this.nodeOptionality = nodeOptionality;
        return this;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public VariableImpl setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    public String getConstantText() {
        return constantText;
    }

    public VariableImpl setConstantText(String constantText) {
        this.constantText = constantText;
        return this;
    }

    public int getConstantInteger() {
        return constantInteger;
    }

    public VariableImpl setConstantInteger(int constantInteger) {
        this.constantInteger = constantInteger;
        return this;
    }

    public VariableImpl setConstantBool(boolean constantBool) {
        this.constantBool = constantBool;
        return this;
    }

    public VariableImpl setConstantDouble(double constantDouble) {
        this.constantDouble = constantDouble;
        return this;
    }

    public VariableImpl setConstantInt64(long constantInt64) {
        this.constantInt64 = constantInt64;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public VariableImpl setExpression(String expression) {
        this.expression = expression;
        return this;
    }

    public ConversionType getFcvClassName() {
        return fcvClassName;
    }

    public VariableImpl setFcvClassName(ConversionType fcvClassName) {
        this.fcvClassName = fcvClassName;
        return this;
    }

    @Override
    public VariableImpl setDataType(DataType type) {
        switch (type) {
            case INT:
                setNodeType(NodeType.INT);
                setNodeOptionality(MUST_EXIST);
                break;
            case STRING:
                setNodeType(NodeType.STRING);
                setNodeOptionality(MUST_EXIST);
                break;
            case BOOL:
                setNodeType(NodeType.BOOL);
                setNodeOptionality(MUST_EXIST);
                break;
            case INT64:
                setNodeType(NodeType.INT64);
                setNodeOptionality(MUST_EXIST);
                break;
            case BINARY:
                setNodeType(NodeType.BINARY);
                setNodeOptionality(MUST_EXIST);
                break;
            case DOUBLE:
                setNodeType(NodeType.DOUBLE);
                setNodeOptionality(MUST_EXIST);
                break;
            case CURRENCY:
                setNodeType(NodeType.CURRENCY);
                setNodeOptionality(MUST_EXIST);
                break;
            case DATE_TIME:
                setNodeType(NodeType.DATETIME);
                setNodeOptionality(MUST_EXIST);
                break;
            case SUB_TREE:
                setNodeType(NodeType.SUB_TREE);
                setNodeOptionality(MUST_EXIST);
                break;
            case OPTIONAL:
                setNodeType(NodeType.SUB_TREE);
                setNodeOptionality(OPTIONAL);
                break;
            case ARRAY:
                setNodeType(NodeType.SUB_TREE);
                setNodeOptionality(ARRAY);
                break;
            default:
                throw new IllegalStateException(type.toString());
        }
        return this;
    }

    @Override
    public VariableImpl setKind(VariableKind kind) {
        switch (kind) {
            case CONSTANT:
                setType(VariableType.CONSTANT);
                break;
            case NUMBERING:
                setType(VariableType.NUMBERING);
                break;
            case CALCULATED:
                setType(VariableType.CALCULATED);
                break;
            case DATA_VARIABLE:
                setType(VariableType.DATA_VARIABLE);
                break;
            case GLOBAL_VARIABLE:
                setType(VariableType.REAL_VARIABLE);
                break;
            case DISCONNECTED:
                setType(VariableType.DISCONNECTED);
                break;
            case OVERFLOW_MESSAGE:
                setType(VariableType.OVERFLOW_OBJECT);
                break;
            case EXTERNAL_CALCULATED:
                setType(EXTERNAL_CALCULATED);
                break;
            case ARRAY_VALUE:
                setType(VariableType.SEARCH_ARRAY_FOR_VALUE);
                break;
            default:
                throw new IllegalStateException(kind.toString());
        }
        return this;
    }

    @Override
    public VariableImpl setValue(String value) {
        return setConstantText(value);
    }

    @Override
    public VariableImpl setValue(int value) {
        return setConstantInteger(value);
    }

    @Override
    public VariableImpl setValue(double value) {
        return setConstantDouble(value);
    }

    @Override
    public VariableImpl setValue(boolean value) {
        return setConstantBool(value);
    }

    @Override
    public VariableImpl setValue(long value) {
        return setConstantInt64(value);
    }

    @Override
    public VariableImpl setScript(String script) {
        return setExpression(script);
    }

    @Override
    public VariableImpl setFlowArea(FlowArea flowArea) {
        this.flowArea = (FlowAreaImpl) flowArea;
        return this;
    }

    @Override
    public Variable setConversion(ConversionType conversion) {
        return setFcvClassName(conversion);
    }

    @Override
    public String getExistingParentId() {
        return existingParentId;
    }

    @Override
    public Variable setExistingParentId(String existingParentId) {
        this.existingParentId = existingParentId;
        return this;
    }

    @Override
    public Variable setDisplacement(Displacement displacement) {
        this.displacement = displacement;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "Variable";
    }

    @Override
    public void export(XmlExporter exporter) {
        if (type == SEARCH_ARRAY_FOR_VALUE) {
            throw new UnsupportedOperationException("This type is not yet supported. type '" + type + "'");
        }

        if (nodeType == SUB_TREE && nodeOptionality == ARRAY && type != DATA_VARIABLE) {
            throw new UnsupportedOperationException("This combination is not yet supported." + nodeOptionality);
        }

        exporter
                .addElementWithStringData("Type", convertVariableTypeToXmlName(type))
                .addElementWithStringData("VarType", convertNodeTypeToXmlName(nodeType, nodeOptionality));

        if (nodeType != SUB_TREE && nodeOptionality == MUST_EXIST) {
            if (type != DATA_VARIABLE) {
                switch (nodeType) {
                    case STRING:
                    case DATETIME:
                        exporter.addElementWithStringData("Content", constantText);
                        break;
                    case INT64:
                        exporter.addElementWithInt64Data("Content", constantInt64);
                        break;
                    case INT:
                        exporter.addElementWithIntData("Content", constantInteger);
                        break;
                    case BOOL:
                        exporter.addElementWithBoolData("Content", constantBool);
                        break;
                    case CURRENCY:
                    case DOUBLE:
                        exporter.addElementWithDoubleData("Content", constantDouble);
                        break;
                    default:
                        throw new UnsupportedOperationException("This combination is not yet supported." +
                                " type '" + type + "', nodeOptionality '" + nodeOptionality + "', nodeType '" + nodeType + "'");
                }
            }
        }

        if (type == DATA_VARIABLE) {
            exporter.addElementWithIntData("InsideFnc", insideFnc);
        }
        if (type == EXTERNAL_CALCULATED) {
            exporter.addElementWithStringData("ExternalScript", constantText);
        }
        if (type == CALCULATED) {
            exporter.addElementWithStringData("Script", expression);
        }
        if (type == OVERFLOW_OBJECT) {
            exporter.addElementWithIface("OverflowFlowArea", flowArea);
        }
        if (fcvClassName != null) {
            exporter.addElementWithStringData("FCVClassName", convertConversionTypeToXmlName(fcvClassName));
        }

        if (fcvClassName == SCRIPT) {
            exporter.beginElement("FCVProps")
                    .addElementWithStringData("InputType", convertNodeTypeToXmlName(nodeType, nodeOptionality))
                    .addElementWithStringData("OutputType", convertNodeTypeToXmlName(STRING, nodeOptionality))
                    .addElementWithStringData("Script", expression)
                    .endElement();
        }

        if (displacement != null) {
            exporter.addElementWithStringData("Displacement", getDisplacementValue(displacement));
        }
    }

    private String getDisplacementValue(Displacement displacement) {
        return switch (displacement) {
            case LOCAL -> "Local";
            case REQUIRED -> "Required";
            case OPTIONAL -> "Optional";
        };
    }
}