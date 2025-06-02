package com.quadient.wfdxml.internal.layoutnodes.tables;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.Cell;
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet;
import com.quadient.wfdxml.api.layoutnodes.tables.HeaderFooterRowSet;
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.HEADER_FOOTER;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.INL_COND;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.REPEATED;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.ROW;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.ROW_SET;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.STRING;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.SWITCH_COND;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.SWITCH_INT;
import static com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl.Type.SWITCH_RANGE;

public class RowSetImpl extends NodeImpl<RowSet> implements GeneralRowSet, HeaderFooterRowSet {
    private final List<Node> subRows = new ArrayList<>();
    private final List<RowSetCondition> rowSetConditions = new ArrayList<>();
    private Variable variable;
    private Type rowSetType = ROW;
    private boolean treatDefaultAsError = false;
    private RowSet defRowSet;

    @Override
    public RowSetImpl addCell(Cell cell) {
        subRows.add(cell);
        return this;
    }

    @Override
    public RowSetImpl addEmptyCell() {
        return addEmptyRowSet();
    }

    @Override
    public RowSetImpl addRowSet(RowSet rowSet) {
        subRows.add(rowSet);
        return this;
    }

    @Override
    public RowSetImpl addEmptyRowSet() {
        subRows.add(null);
        return this;
    }

    public Type getRowSetType() {
        return rowSetType;
    }

    public RowSetImpl setRowSetType(Type rowSetType) {
        this.rowSetType = rowSetType;
        return this;
    }

    @Override
    public RowSetImpl addLineForSelectByCondition(Variable variable, RowSet rowSet) {
        rowSetConditions.add(new RowSetCondition(variable, rowSet));
        return this;
    }

    @Override
    public RowSetImpl addLineForSelectByInlineCondition(String script, RowSet rowSet) {
        rowSetConditions.add(new RowSetCondition(script, rowSet));
        return this;
    }

    @Override
    public RowSetImpl setDefaultRowSet(RowSet defRowSet) {
        this.defRowSet = defRowSet;
        return this;
    }

    @Override
    public RowSetImpl setDefaultError(boolean defaultError) {
        this.treatDefaultAsError = defaultError;
        return this;
    }

    @Override
    public RowSetImpl setType(RowSet.Type rowSetType) {
        switch (rowSetType) {
            case SINGLE_ROW:
                return setRowSetType(ROW);
            case MULTIPLE_ROWS:
                return setRowSetType(ROW_SET);
            case REPEATED:
                return setRowSetType(REPEATED);
            case SELECT_BY_INTEGER:
                return setRowSetType(SWITCH_INT);
            case SELECT_BY_INTERVAL:
                return setRowSetType(SWITCH_RANGE);
            case SELECT_BY_CONDITION:
                return setRowSetType(SWITCH_COND);
            case HEADER_AND_FOOTER:
                return setRowSetType(HEADER_FOOTER);
            case SELECT_BY_TEXT:
                return setRowSetType(STRING);
            case SELECT_BY_INLINE_CONDITION:
                return setRowSetType(INL_COND);
            default:
                throw new IllegalArgumentException(rowSetType.toString());
        }
    }

    @Override
    public String getXmlElementName() {
        return "RowSet";
    }

    public HeaderFooterRowSet initAsHeaderFooterRowSet() {
        setRowSetType(HEADER_FOOTER);
        subRows.clear();
        addEmptyRowSet();
        addEmptyRowSet();
        addEmptyRowSet();
        addEmptyRowSet();
        addEmptyRowSet();
        return this;
    }

    @Override
    public RowSetImpl setFirstHeader(RowSet rowset) {
        subRows.set(0, rowset);
        return this;
    }

    @Override
    public RowSetImpl setHeader(RowSet rowset) {
        subRows.set(1, rowset);
        return this;
    }

    @Override
    public RowSetImpl setBody(RowSet rowset) {
        subRows.set(2, rowset);
        return this;
    }

    @Override
    public RowSetImpl setFooter(RowSet rowset) {
        subRows.set(3, rowset);
        return this;
    }

    @Override
    public RowSetImpl setLastFooter(RowSet rowset) {
        subRows.set(4, rowset);
        return this;
    }

    public Variable getVariable() {
        return variable;
    }

    @Override
    public RowSetImpl setVariable(Variable variable) {
        this.variable = variable;
        return this;
    }

    @Override
    public RowSetImpl setRowSet(RowSet rowSet) {
        subRows.clear();
        subRows.add(rowSet);
        return this;
    }

    @Override
    public void export(XmlExporter exporter) {

        switch (rowSetType) {
            case ROW:
                exporter.addElementWithStringData("RowSetType", "Row");
                break;
            case ROW_SET:
                exporter.addElementWithStringData("RowSetType", "RowSet");
                break;
            case REPEATED:
                exporter.addElementWithStringData("RowSetType", "Repeated");
                break;
            case SWITCH_COND:
                exporter.addElementWithStringData("RowSetType", "Condition");
                break;
            case HEADER_FOOTER:
                exporter.addElementWithStringData("RowSetType", "HeaderFooter");
                break;
            case INL_COND:
                exporter.addElementWithStringData("RowSetType", "InlCond");
                break;
            default:
                throw new UnsupportedOperationException("Unsupported rowSetType '" + rowSetType + "'");
        }

        switch (rowSetType) {
            case ROW:
            case ROW_SET:
            case HEADER_FOOTER:
                for (Node subRow : subRows) {
                    exporter.addElementWithIface("SubRowId", subRow);
                }
                break;
            case REPEATED:
                for (Node subRow : subRows) {
                    exporter.addElementWithIface("SubRowId", subRow);
                }
                exporter.addElementWithIface("VariableId", variable);
                break;

            case SWITCH_COND:
                for (RowSetCondition condition : rowSetConditions) {
                    exporter.beginElement("RowSetCondition")
                            .addElementWithIface("SubRowId", condition.getRowSet())
                            .addElementWithIface("VariableId", (Node) condition.getCondition())
                            .endElement();
                }
                exporter.beginElement("RowSetCondition")
                        .addElementWithIface("SubRowId", defRowSet)
                        .addElement("VariableId")
                        .endElement();

                break;

            case INL_COND:
                for (RowSetCondition condition : rowSetConditions) {
                    exporter.beginElement("RowSetCondition")
                            .addElementWithIface("SubRowId", condition.getRowSet())
                            .addElementWithStringData("Condition", condition.getCondition().toString())
                            .endElement();
                }
                exporter.beginElement("RowSetCondition")
                        .addElementWithIface("SubRowId", defRowSet)
                        .addElement("Condition")
                        .endElement();

                exporter.addElementWithIface("VariableId", variable);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported rowSetType '" + rowSetType + "'");
        }


        if (rowSetType == ROW) {
            exporter.addElement("VariableId");//  exporter.addElementWithIface("VariableId", (LANodeIface *)mKeepWithPrevious);
            exporter.addElement("VariableId");//  exporter.addElementWithIface("VariableId", (LANodeIface *)mKeepWithNext);
            exporter.addElement("VariableId");//  exporter.addElementWithIface("VariableId", (LANodeIface *)mBreakBefore);
            exporter.addElement("VariableId");//  exporter.addElementWithIface("VariableId", (LANodeIface *)mBreakAfter);
        }

        switch (rowSetType) {
            case SWITCH_INT:
            case SWITCH_RANGE:
            case SWITCH_COND:
            case STRING:
            case INL_COND:
                exporter.addElementWithBoolData("TreatDefaultAsError", treatDefaultAsError);
                break;
            default:
                //do nothing according to Designer
        }


    }

    enum Type {
        ROW,
        ROW_SET,
        REPEATED,
        SWITCH_INT,
        SWITCH_RANGE,
        SWITCH_COND,
        HEADER_FOOTER,
        STRING,
        INL_COND,
        EMPTY_ROW,
    }

    private class RowSetCondition {
        private final Object condition;
        private final RowSet rowSet;

        public RowSetCondition(Object condition, RowSet rowSet) {
            this.condition = condition;
            this.rowSet = rowSet;
        }

        public Object getCondition() {
            return condition;
        }

        public RowSet getRowSet() {
            return rowSet;
        }
    }
}