package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.Pages;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.List;

import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageConditionType.SIMPLE;
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageConditionType.SWITCH_COND;
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageSelectionType.DATA_VARIABLE;
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageSelectionType.VARIABLE;

public class PagesImpl extends Tree<Pages> implements Pages {

    private PageSelectionType pageSelectionType = PageSelectionType.SIMPLE;
    private PageConditionType type = SIMPLE;
    private PageImpl page = null;
    private Flow mainFlow = null;
    private List<Flow> interactiveFlows = null;

    public static String pageConditionTypeToXml(PageConditionType type) {
        switch (type) {
            case SIMPLE:
                return "Simple";
            case SWITCH_INT:
                return "Integer";
            case SWITCH_RANGE:
                return "Interval";
            case SWITCH_COND:
                return "Condition";
            case STRING:
                return "String";
            case INL_COND:
                return "InlCond";
            case CONTENT:
                return "Content";
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }

    public PageSelectionType getPageSelectionType() {
        return pageSelectionType;
    }

    public PagesImpl setPageSelectionType(PageSelectionType pageSelectionType) {
        this.pageSelectionType = pageSelectionType;
        return this;
    }

    @Override
    public PagesImpl setPageOrder(PageOrder pageOrder) {
        switch (pageOrder) {
            case SIMPLE:
                setPageSelectionType(PageSelectionType.SIMPLE);
                break;
            case VARIABLE_SELECTION:
                setPageSelectionType(VARIABLE);
                break;
            case DATA_VARIABLE_SELECTION:
                setPageSelectionType(DATA_VARIABLE);
                break;
            default:
                throw new IllegalArgumentException(pageOrder.toString());
        }
        return this;
    }

    public PageConditionType getType() {
        return type;
    }

    public PagesImpl setType(PageConditionType type) {
        this.type = type;
        return this;
    }

    public PageImpl getPage() {
        return page;
    }

    public PagesImpl setPage(PageImpl page) {
        this.page = page;
        return this;
    }

    @Override
    public PagesImpl setType(Pages.PageConditionType type) {
        return setType(PageConditionType.convertFromApi(type));
    }

    @Override
    public PagesImpl setStartPage(Page page) {
        return setPage((PageImpl) page);
    }

    @Override
    public Pages setMainFlow(Flow mainFlow) {
        this.mainFlow = mainFlow;
        return this;
    }

    @Override
    public Pages setInteractiveFlows(List<Flow> interactiveFlows) {
        this.interactiveFlows = interactiveFlows;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "Pages";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithStringData("SelectionType", pageSelectionTypeToXml());
        if (mainFlow != null){
            exporter.addElementWithIface("MainFlow", mainFlow);
            exporter.addElementWithBoolData("UseAnotherFlowAsInteractiveMainFlow", false);
        }
        if (interactiveFlows != null){
            for (Flow flow:interactiveFlows) {
                exporter.beginElement("InteractiveFlow");
                exporter.addElementWithIface("FlowId", flow);
                exporter.addElementWithStringData("FlowType", "Normal");
                exporter.endElement();
            }
        }
        if (pageSelectionType == VARIABLE) {
            exporter.addElementWithStringData("ConditionType", pageConditionTypeToXml(type));
            //noinspection StatementWithEmptyBody
            if (type == SIMPLE) {
                exporter.addElementWithIface("FirstPageId", page);
            } else {
                //DefaultPageId and other stuff...
            }
        }

        if (type != SIMPLE && type != SWITCH_COND) {
            throw new IllegalArgumentException("Type '" + type + "' is not supported.");
        }

    }

    public String pageSelectionTypeToXml() {
        switch (pageSelectionType) {
            case SIMPLE:
                return "Simple";
            case VARIABLE:
                return "Variable";
            case DATA_VARIABLE:
                return "DataVariable";
            default:
                throw new IllegalArgumentException(pageSelectionType.toString());
        }
    }

    enum PageSelectionType {
        SIMPLE,
        VARIABLE,
        DATA_VARIABLE
    }

    enum PageConditionType {
        SIMPLE,
        SWITCH_INT,
        SWITCH_RANGE,
        SWITCH_COND,
        STRING,
        INL_COND,
        CONTENT;

        public static PageConditionType convertFromApi(Pages.PageConditionType type) {
            switch (type) {
                case SIMPLE:
                    return SIMPLE;
                case SELECT_BY_INTEGER:
                    return SWITCH_INT;
                case SELECT_BY_INTERVAL:
                    return SWITCH_RANGE;
                case SELECT_BY_CONDITION:
                    return SWITCH_COND;
                case SELECT_BY_TEXT:
                    return STRING;
                case SELECT_BY_INLINE_CONDITION:
                    return INL_COND;
                case SELECT_BY_CONTENT:
                    return CONTENT;
                default:
                    throw new IllegalArgumentException(type.toString());
            }
        }
    }
}