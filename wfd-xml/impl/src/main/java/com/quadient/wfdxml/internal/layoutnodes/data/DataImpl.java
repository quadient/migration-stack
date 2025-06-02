package com.quadient.wfdxml.internal.layoutnodes.data;

import com.quadient.wfdxml.api.data.DataDefinition;
import com.quadient.wfdxml.api.layoutnodes.data.Data;
import com.quadient.wfdxml.api.layoutnodes.data.SystemVariables;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition;
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.HashMap;
import java.util.List;

import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.DIRECT_EXTERNAL_FLOW_CUSTOM_PROPERTY;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.DIRECT_EXTERNAL_FLOW_LOCATION;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GLOBAL_PAGE_COUNT;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GLOBAL_PAGE_COUNTER;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GLOBAL_SHEET_COUNT;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GLOBAL_SHEET_COUNTER;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GROUP_SHEET_COUNT;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GROUP_SHEET_COUNTER;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.GROUT_INDEX;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.JOB_NAME;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.JOB_STARTED;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.LANGUAGE;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.OVERFLOW;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGES_PER_RECORD;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGES_PER_SUB_RECORD;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGE_ARRAY_INDEX;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGE_COUNTER;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGE_INDEX;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGE_NAME;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PAGE_SUB_COUNTER;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PREVIOUS_PAGE_INDEX;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PREVIOUS_PAGE_NAME;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.PRINTING_FACE;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.REAL_GROUP_SHEET_COUNT;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.REAL_GROUP_SHEET_COUNTER;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.RECORD_INDEX;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.SERIE_INDEX;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.SKIN;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.TRUE;
import static com.quadient.wfdxml.api.layoutnodes.data.SystemVariables.VALUE_INDEX;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.CONSTANT;
import static com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType.DATA_VARIABLE;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality.MUST_EXIST;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.BOOL;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.DATETIME;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.INT;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.STRING;
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE;
import static java.util.Arrays.asList;

public class DataImpl extends Tree implements Data {

    private final HashMap<SystemVariables, VariableImpl> map = new HashMap<>();
    private VariableImpl systemVariableArray;
    private boolean dataImported = false;

    public DataImpl() {
        initDefaultVariables();
    }

    private void initDefaultVariables() {
        createSystemVariableArray();

        createSystemVariable(-11, INT, "PageCounter", PAGE_COUNTER);
        createSystemVariable(-12, BOOL, "Overflow", OVERFLOW);
        createSystemVariable(-13, STRING, "JobName", JOB_NAME);
        createSystemVariable(-14, INT, "PageIndex", PAGE_INDEX);
        createSystemVariable(-15, INT, "PreviousPageIndex", PREVIOUS_PAGE_INDEX);
        createSystemVariable(-16, STRING, "PageName", PAGE_NAME);
        createSystemVariable(-17, STRING, "PreviousPageName", PREVIOUS_PAGE_NAME);
        createSystemVariable(-18, INT, "RecordIndex", RECORD_INDEX);
        createSystemVariable(-19, INT, "PagesPerRecord", PAGES_PER_RECORD);
        createSystemVariable(-20, INT, "GlobalPageCounter", GLOBAL_PAGE_COUNTER);
        createSystemVariable(-21, INT, "PageArrayIndex", PAGE_ARRAY_INDEX);
        createSystemVariable(-22, INT, "GlobalSheetCounter", GLOBAL_SHEET_COUNTER);
        createSystemVariable(-23, INT, "GlobalSheetCount", GLOBAL_SHEET_COUNT);
        createSystemVariable(-24, INT, "GlobalPageCount", GLOBAL_PAGE_COUNT);
        createSystemVariable(-25, DATETIME, "JobStarted", JOB_STARTED);
        createSystemVariable(-26, BOOL, "True", TRUE);
        createSystemVariable(-27, INT, "PagesPerSubRecord", PAGES_PER_SUB_RECORD);
        createSystemVariable(-28, INT, "PageSubCounter", PAGE_SUB_COUNTER);
        createSystemVariable(-30, INT, "GroupSheetCounter", GROUP_SHEET_COUNTER);
        createSystemVariable(-29, BOOL, "PrintingFace", PRINTING_FACE);
        createSystemVariable(-31, INT, "GroupIndex", GROUT_INDEX);
        createSystemVariable(-32, INT, "SerieIndex", SERIE_INDEX);
        createSystemVariable(-33, INT, "ValueIndex", VALUE_INDEX);
        createSystemVariable(-34, STRING, "Language", LANGUAGE);
        createSystemVariable(-35, INT, "GroupSheetCount", GROUP_SHEET_COUNT);
        createSystemVariable(-36, STRING, "Skin", SKIN);
        createSystemVariable(-37, INT, "RealGroupSheetCounter", REAL_GROUP_SHEET_COUNTER);
        createSystemVariable(-38, INT, "RealGroupSheetCount", REAL_GROUP_SHEET_COUNT);
        createSystemVariable(-39, STRING, "DirectExternalFlowLocation", DIRECT_EXTERNAL_FLOW_LOCATION);
        createSystemVariable(-40, STRING, "DirectExternalFlowCustomProperty", DIRECT_EXTERNAL_FLOW_CUSTOM_PROPERTY);
    }

    private void createSystemVariableArray() {
        systemVariableArray = new VariableImpl()
                .setNodeType(SUB_TREE)
                .setNodeOptionality(MUST_EXIST)
                .setType(DATA_VARIABLE)
                .setInsideFnc(-10);
        systemVariableArray.setName("SystemVariable");

        children.add(systemVariableArray);
    }

    private void createSystemVariable(int insideFnc, NodeType nodeType, String name, SystemVariables type) {
        VariableImpl var = new VariableImpl().setInsideFnc(insideFnc).setNodeType(nodeType);
        var.setName(name);
        systemVariableArray.children.add(var);
        map.put(type, var);
    }

    public VariableImpl getSystemVariableArray() {
        return systemVariableArray;
    }


    @Override
    public Variable getSystemVariable(SystemVariables type) {
        return map.get(type);
    }

    @Override
    public DataImpl importDataDefinition(DataDefinition dataDefinition) {
        assertDataWasNotImported();
        WorkFlowTreeDefinition wtd = (WorkFlowTreeDefinition) dataDefinition;
        populate(wtd.getSubNodes(), this);
        return this;
    }

    private void assertDataWasNotImported() {
        if (dataImported) {
            throw new IllegalStateException("Method 'importDataDefinition' can be called only once.");
        }
        dataImported = true;
    }

    private void populate(List<WorkFlowTreeDefinition> wtds, Tree parent) {
        for (WorkFlowTreeDefinition wtd : wtds) {
            populate(wtd, parent);
        }
    }

    private void populate(WorkFlowTreeDefinition wtd, Tree parent) {
        VariableImpl variable = addPopulatedVariable(wtd.getCaption(), DATA_VARIABLE, wtd.getNodeType(), wtd.getNodeOptionality(), 0, parent);
        switch (wtd.getNodeOptionality()) {
            case OPTIONAL:
            case ARRAY:
                populateOptionalOrArray(wtd, variable);
                break;
            case MUST_EXIST:
                populate(wtd.getSubNodes(), variable);
                break;
            default:
                throw new IllegalStateException(wtd.getNodeOptionality().toString());
        }
    }

    private void populateOptionalOrArray(WorkFlowTreeDefinition wtd, VariableImpl variable) {
        Tree value = addPopulatedVariable("Value", DATA_VARIABLE, SUB_TREE, MUST_EXIST, -1, variable);
        switch (wtd.getNodeOptionality()) {
            case ARRAY:
                NodeImpl count = addPopulatedVariable("Count", DATA_VARIABLE, INT, MUST_EXIST, -2, variable);
                break;
            case OPTIONAL:
                NodeImpl exists = addPopulatedVariable("Exists", DATA_VARIABLE, BOOL, MUST_EXIST, -2, variable);
                break;
            default:
                throw new IllegalStateException("This optionality can never be here '" + wtd.getNodeOptionality() + "'");
        }
        populate(wtd.getSubNodes(), value);
    }

    private VariableImpl addPopulatedVariable(String name, VariableType type, NodeType nodeType, NodeOptionality nodeOptionality, int insideFnc, Tree parentOfVariable) {
        VariableImpl var = new VariableImpl();
        var
                .setType(type)
                .setNodeType(nodeType)
                .setNodeOptionality(nodeOptionality)
                .setInsideFnc(insideFnc)
                .setName(name);
        parentOfVariable.children.add(var);
        return var;
    }


    @Override
    public String getXmlElementName() {
        return "Data";
    }

    @Override
    public void export(XmlExporter exporter) {
    }

    @Override
    public VariableImpl addVariable() {
        VariableImpl var = new VariableImpl().setType(CONSTANT).setNodeType(STRING);
        children.add(var);
        return var;
    }

    @Override
    public VariableImpl findVariable(String... path) {
        return findVariable(asList(path), 0, children);
    }

    private VariableImpl findVariable(List<String> path, int indexToFind, List<NodeImpl> children) {
        String varName = path.get(indexToFind);
        for (NodeImpl child : children) {
            VariableImpl childVar = (VariableImpl) child;
            if (varName.equals(childVar.getName())) {
                if (indexToFind == path.size() - 1) {
                    return childVar;
                } else {
                    List<NodeImpl> childrenFromValue = getChildren(childVar);
                    return findVariable(path, indexToFind + 1, childrenFromValue);
                }
            }
        }
        throw new IllegalArgumentException("Variable not found. For whole path = " + path + "." +
                " Part of path, that was successfully found " + path.subList(0, indexToFind) + "." +
                " Variable which was not found = '" + varName + "'");
    }

    private List<NodeImpl> getChildren(VariableImpl var) {
        NodeOptionality optionality = var.getNodeOptionality();
        switch (optionality) {
            case MUST_EXIST:
                return var.children;
            case OPTIONAL:
            case ARRAY:
                VariableImpl valueSubNode = (VariableImpl) var.children.get(0);
                if (!"Value".equals(valueSubNode.getName())) {
                    throw new IllegalStateException("Variable with name 'Value' was expected but found '" + var.getName() + "'");
                }
                return valueSubNode.children;
            default:
                throw new IllegalStateException(optionality.toString());
        }
    }
}
