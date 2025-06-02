package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.Pages.PageConditionType;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface Page extends Node<Page> {

    FlowArea addFlowArea();

    ImageArea addImageArea();

    PathObject addPathObject();

    PathObject addLine(double x1, double y1, double x2, double y2);

    Page setType(PageConditionType type);

    Page addLineForSelectByCondition(Variable variable, int rollback, Page page);

    Page setDefaultRollback(int defaultRollback);

    Page setDefaultError(boolean defaultError);

    Page setDefaultPage(Page defaultPage);

    Page setWidth(double width);

    Page setHeight(double height);

    BarcodeFactory getBarcodeFactory();
}