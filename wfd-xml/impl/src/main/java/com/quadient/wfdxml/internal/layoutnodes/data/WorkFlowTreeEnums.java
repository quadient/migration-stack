package com.quadient.wfdxml.internal.layoutnodes.data;

public class WorkFlowTreeEnums {

    public static String typeToString(NodeType nodeType) {
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
            case SUB_TREE:
                return "SubTree";
            case BINARY:
                return "Binary";
            default:
                throw new IllegalArgumentException(nodeType.toString());
        }
    }

    public enum NodeType {
        STRING,
        INT,
        INT64,
        CURRENCY,
        DOUBLE,
        DATETIME,
        BOOL,
        SUB_TREE,
        BINARY,
    }

    public enum NodeOptionality {
        OPTIONAL,
        MUST_EXIST,
        ARRAY,
    }
}