package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.layoutnodes.flow.ParagraphImpl;
import com.quadient.wfdxml.internal.layoutnodes.support.FlowContent;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.CONDITION;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.CONTEXT;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.DIRECT_EXTERNAL;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.DYNAMIC_EXTERNAL;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.EXTERNAL;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.FIRST_FITTING;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.FIRST_FITTING_AUTO;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.INL_COND;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.INTEGER;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.INTERVAL;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.LANGUAGE;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.OVERFLOWABLE_VARIABLE_FORMATTED;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.REPEATED;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.SIMPLE;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.STRING;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.TEXT;
import static com.quadient.wfdxml.internal.layoutnodes.FlowImpl.Type.VARIABLE_FORMATTED;

public class FlowImpl extends NodeImpl<Flow> implements Flow {
    private final List<FlowCondition> flowConditions = new ArrayList<>();
    private Type type = SIMPLE;
    private Variable variable;
    private boolean sectionFlow = false;
    private FlowContent flowContent = new FlowContent();
    private Flow defFlow;
    private boolean treatDefaultAsError = false;
    private String location;

    public static String convertTypeToXmlName(Type type) {
        switch (type) {
            case SIMPLE:
                return "Simple";
            case REPEATED:
                return "Repeated";
            case EXTERNAL:
                return "External";
            case DIRECT_EXTERNAL:
                return "DirectExternal";
            case DYNAMIC_EXTERNAL:
                return "DynamicExternal";
            case INTEGER:
                return "Integer";
            case INTERVAL:
                return "Interval";
            case CONDITION:
                return "Condition";
            case TEXT:
                return "String";
            case STRING:
                return "RawString";
            case INL_COND:
                return "InlCond";
            case FIRST_FITTING:
                return "FirstFitting";
            case FIRST_FITTING_AUTO:
                return "FirstFittingAuto";
            case VARIABLE_FORMATTED:
                return "VariableFormatted";
            case OVERFLOWABLE_VARIABLE_FORMATTED:
                return "OverflowableVariableFormatted";
            case CONTEXT:
                return "Content";
            case LANGUAGE:
                return "Language";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    @Override
    public ParagraphImpl addParagraph() {
        return flowContent.addParagraph(new ParagraphImpl(this));
    }

    public boolean isSectionFlow() {
        return sectionFlow;
    }

    @Override
    public FlowImpl setSectionFlow(boolean sectionFlow) {
        this.sectionFlow = sectionFlow;
        return this;
    }

    @Override
    public FlowImpl setType(Flow.Type type) {
        switch (type) {
            case SIMPLE:
                return setFlowType(SIMPLE);
            case SELECT_BY_INTEGER:
                return setFlowType(INTEGER);
            case SELECT_BY_INTERVAL:
                return setFlowType(INTERVAL);
            case SELECT_BY_CONDITION:
                return setFlowType(CONDITION);
            case REPEATED:
                return setFlowType(REPEATED);
            case EXTERNAL:
                return setFlowType(EXTERNAL);
            case SELECT_BY_TEXT:
                return setFlowType(TEXT);
            case SELECT_BY_INLINE_CONDITION:
                return setFlowType(INL_COND);
            case FIRST_FITTING:
                return setFlowType(FIRST_FITTING);
            case FIRST_FITTING_AUTO:
                return setFlowType(FIRST_FITTING_AUTO);
            case VARIABLE_FORMATTED:
                return setFlowType(VARIABLE_FORMATTED);
            case OVERFLOWABLE_VARIABLE_FORMATTED:
                return setFlowType(OVERFLOWABLE_VARIABLE_FORMATTED);
            case SELECT_BY_CONTENT:
                return setFlowType(CONTEXT);
            case SELECT_BY_LANGUAGE:
                return setFlowType(LANGUAGE);
            case DIRECT_EXTERNAL:
                return setFlowType(DIRECT_EXTERNAL);
            case DYNAMIC_EXTERNAL:
                return setFlowType(DYNAMIC_EXTERNAL);
            case SELECT_BY_STRING:
                return setFlowType(STRING);
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }

    @Override
    public FlowImpl addLineForSelectByInlineCondition(String script, Flow flow) {
        flowConditions.add(new FlowCondition(script, flow));
        return this;
    }

    @Override
    public FlowImpl addLineForSelectByCondition(Variable variable, Flow flow) {
        flowConditions.add(new FlowCondition(variable, flow));
        return this;
    }

    @Override
    public FlowImpl setDefaultFlow(Flow flow) {
        this.defFlow = flow;
        return this;
    }

    @Override
    public FlowImpl setDefaultError(boolean defaultError) {
        this.treatDefaultAsError = defaultError;
        return this;
    }

    @Override
    public Flow setLocation(String location) {
        this.location = location;
        return this;
    }

    public Flow getDefFlow() {
        return defFlow;
    }

    public boolean isTreatDefaultAsError() {
        return treatDefaultAsError;
    }

    @Override
    public String getXmlElementName() {
        return "Flow";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithStringData("Type", convertTypeToXmlName(type));
        flowContent.export(exporter);

        switch (type) {
            case SIMPLE, REPEATED, CONDITION, INL_COND, VARIABLE_FORMATTED:
                break;
            case DIRECT_EXTERNAL: {
                if (location == null || location.isBlank()) {
                    throw new UnsupportedOperationException("DirectExternal Flow requires a location");
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unsupported Flow type '" + type + "'");
        }

        if (type == INL_COND || type == REPEATED || type == VARIABLE_FORMATTED) {
            exporter.addElementWithIface("Variable", variable);
        }

        if (type == INL_COND || type == CONDITION) {
            for (FlowCondition con : flowConditions) {
                exporter.beginElement("Condition");

                switch (type) {
                    case CONDITION:
                        exporter.addIfaceAttribute("VarId", con.getCondition());
                        break;
                    case INL_COND:
                        exporter.addStringAttribute("Value", con.getCondition().toString());
                        break;
                }

                exporter.addPCData(con.getFlow());
                exporter.endElement();
            }

            exporter.addElementWithIface("Default", defFlow)
                    .addElementWithBoolData("TreatDefaultAsError", treatDefaultAsError);
        }

        if (type == DIRECT_EXTERNAL) {
            exporter.addElementWithStringData("ExternalLocation", location);
        }

        exporter.addElementWithBoolData("SectionFlow", sectionFlow);
    }

    public Type getFlowType() {
        return type;
    }

    public FlowImpl setFlowType(Type type) {
        this.type = type;
        return this;
    }

    public FlowContent getFlowContent() {
        return flowContent;
    }

    public void setFlowContent(FlowContent flowContent) {
        this.flowContent = flowContent;
    }

    public Variable getVariable() {
        return variable;
    }

    @Override
    public FlowImpl setVariable(Variable variable) {
        this.variable = variable;
        return this;
    }

    enum Type {
        SIMPLE,
        INTEGER,
        INTERVAL,
        CONDITION,
        REPEATED,
        EXTERNAL,
        TEXT,
        INL_COND,
        FIRST_FITTING,
        FIRST_FITTING_AUTO,
        VARIABLE_FORMATTED,
        OVERFLOWABLE_VARIABLE_FORMATTED,
        CONTEXT,
        LANGUAGE,
        DIRECT_EXTERNAL,
        DYNAMIC_EXTERNAL,
        STRING
    }

    private class FlowCondition {
        private final Flow flow;
        private final Object condition;

        public FlowCondition(Object condition, Flow flow) {
            this.condition = condition;
            this.flow = flow;
        }

        public Flow getFlow() {
            return flow;
        }

        public Object getCondition() {
            return condition;
        }
    }
}