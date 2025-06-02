package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.Pages;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageConditionType;
import com.quadient.wfdxml.internal.layoutnodes.support.PageCondition;
import com.quadient.wfdxml.internal.module.layout.LayoutImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.internal.layoutnodes.PageImpl.ConditionType.CONTINUE_AFTER_REPETITION;
import static com.quadient.wfdxml.internal.layoutnodes.PageImpl.ConditionType.NEXT_PAGE;
import static com.quadient.wfdxml.internal.layoutnodes.PageImpl.ConditionType.START_REPETITION;
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageConditionType.SIMPLE;
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.PageConditionType.SWITCH_COND;
import static com.quadient.wfdxml.internal.layoutnodes.PagesImpl.pageConditionTypeToXml;
import static java.util.Arrays.asList;

public class PageImpl extends Tree<Page> implements Page {

    private final PageConditionType[] type = new PageConditionType[3];
    private final Page[] page = new Page[3];
    private final int[] rollback = new int[3];
    private final Variable[] variable = new Variable[3];
    private final List<List<PageCondition>> pageConditions = asList(null, null, null);
    private final boolean[] treatDefaultAsError = new boolean[3];
    private final LayoutImpl layout;
    private double width = 0;
    private double height = 0;

    public PageImpl(LayoutImpl layout) {
        this.layout = layout;
        for (int i = 0; i < 3; i++) {
            variable[i] = null;
            page[i] = null;
            rollback[i] = 0;
            type[i] = SIMPLE;
            pageConditions.set(i, new ArrayList<PageCondition>());
            treatDefaultAsError[i] = false;
        }
    }

    @Override
    public FlowAreaImpl addFlowArea() {
        FlowAreaImpl flowArea = new FlowAreaImpl();
        children.add(flowArea);
        return flowArea;
    }

    @Override
    public ImageObjectImpl addImageArea() {
        ImageObjectImpl imageObject = new ImageObjectImpl();
        children.add(imageObject);
        return imageObject;
    }

    @Override
    public PathObjectImpl addPathObject() {
        PathObjectImpl pathObject = new PathObjectImpl();
        pathObject.setLineFillStyle(layout == null ? null : layout.getDefFillStyle());
        children.add(pathObject);
        return pathObject;
    }

    @Override
    public PathObjectImpl addLine(double x1, double y1, double x2, double y2) {
        PathObjectImpl pathObject = addPathObject();
        pathObject
                .setPositionAndSize(x1, y1, (x2 - x1), (y2 - y1))
                .addLineTo((x2 - x1), (y2 - y1));
        return pathObject;
    }

    @Override
    public PageImpl setType(Pages.PageConditionType type) {
        this.type[0] = PageConditionType.convertFromApi(type);
        return this;
    }

    @Override
    public PageImpl addLineForSelectByCondition(Variable variable, int rollback, Page page) {
        pageConditions.get(0).add(new PageCondition(variable, rollback, page));
        return this;
    }

    @Override
    public PageImpl setDefaultRollback(int defaultRollback) {
        rollback[0] = defaultRollback;
        return this;
    }

    @Override
    public PageImpl setDefaultError(boolean defaultError) {
        treatDefaultAsError[0] = defaultError;
        return this;
    }

    @Override
    public PageImpl setDefaultPage(Page defaultPage) {
        page[0] = defaultPage;
        return this;
    }

    @Override
    public Page setWidth(double width) {
        this.width = width;
        return this;
    }

    @Override
    public Page setHeight(double height) {
        this.height = height;
        return this;
    }

    @Override
    public BarcodeFactoryImpl getBarcodeFactory() {
        return new BarcodeFactoryImpl(children, layout.getDefTextStyle());
    }

    @Override
    public String getXmlElementName() {
        return "Page";
    }

    @Override
    public void export(XmlExporter exporter) {
        exportPageProperties(exporter, NEXT_PAGE);
        if (width != 0) exporter.addElementWithDoubleData("Width", width);
        if (height != 0) exporter.addElementWithDoubleData("Height", height);
        exportPageProperties(exporter, START_REPETITION);
        exportPageProperties(exporter, CONTINUE_AFTER_REPETITION);
    }

    private void exportPageProperties(XmlExporter exporter, ConditionType condType) {
        exporter.addElementWithStringData("ConditionType", pageConditionTypeToXml(type[condType.index]));

        if (type[condType.index] != SIMPLE) {
            exporter.addElementWithIface("DefaultPageId", page[condType.index]);
            if (condType == NEXT_PAGE) {
                exporter.addElementWithIntData("DefaultRollback", rollback[condType.index]);
            }
            if (type[condType.index] != SWITCH_COND) {
                exporter.addElementWithIface("VariableId", variable[condType.index]);
            }

            for (PageCondition pc : pageConditions.get(condType.index)) {

                exporter.beginElement("PageCondition");
                switch (type[condType.index]) {
                    case SWITCH_INT: {
                        exporter.addElementWithIntData("Value", pc.integer);
                    }
                    break;
                    case SWITCH_RANGE: {
                        exporter.addElementWithIntData("RangeBegin", pc.integer);
                        exporter.addElementWithIntData("RangeEnd", pc.end);
                    }
                    break;
                    case SWITCH_COND: {
                        exporter.addElementWithIface("ConditionId", pc.variable);
                    }
                    break;
                    case STRING:
                    case INL_COND: {
                        exporter.addElementWithStringData("Condition", pc.condStr);
                    }
                    break;
                    default:
                        break;
                }
                if (condType == NEXT_PAGE) exporter.addElementWithIntData("Rollback", pc.rollback);
                exporter.addElementWithIface("PageId", pc.page);
                exporter.endElement();
            }
        } else {
            exporter.addElementWithIface("NextPageId", page[condType.index]);
        }

        switch (type[condType.index]) {
            case SWITCH_INT:
            case SWITCH_RANGE:
            case SWITCH_COND:
            case STRING:
            case INL_COND:
            case CONTENT:
                exporter.addElementWithBoolData("TreatDefaultAsError", treatDefaultAsError[condType.index]);
                break;
            default:
                //do nothing
        }
    }

    enum ConditionType {
        NEXT_PAGE(0),
        START_REPETITION(1),
        CONTINUE_AFTER_REPETITION(2);

        public int index;

        ConditionType(int index) {
            this.index = index;
        }
    }
}