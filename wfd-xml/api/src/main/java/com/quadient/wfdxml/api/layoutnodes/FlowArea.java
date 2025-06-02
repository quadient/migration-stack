package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;

public interface FlowArea extends LayoutObject<FlowArea> {

    FlowArea setFlow(Flow flow);

    FlowArea setBorderStyle(BorderStyle borderStyle);

    FlowArea setFlowToNextPage(boolean flowToNextPage);

    FlowArea setBorderType(BorderType type);

    enum BorderType {
        CONTENT_ONLY,
        CONTENT_WITH_LINE_GAB,
        FULL_AREA
    }
}