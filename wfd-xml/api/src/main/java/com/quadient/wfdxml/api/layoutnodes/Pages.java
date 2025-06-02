package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

import java.util.List;

public interface Pages extends Node<Pages> {

    Pages setPageOrder(PageOrder pageOrder);

    Pages setType(PageConditionType type);

    Pages setStartPage(Page page);

    Pages setMainFlow(Flow mainFlow);

    Pages setInteractiveFlows(List<Flow> interactiveFlows);

    enum PageOrder {
        SIMPLE,
        VARIABLE_SELECTION,
        DATA_VARIABLE_SELECTION,
    }

    enum PageConditionType {
        SIMPLE,
        SELECT_BY_INTEGER,
        SELECT_BY_INTERVAL,
        SELECT_BY_CONDITION,
        SELECT_BY_TEXT,
        SELECT_BY_INLINE_CONDITION,
        SELECT_BY_CONTENT,
    }
}