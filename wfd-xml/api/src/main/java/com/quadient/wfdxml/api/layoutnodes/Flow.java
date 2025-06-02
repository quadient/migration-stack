package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.flow.Paragraph;

public interface Flow extends Node<Flow> {

    Paragraph addParagraph();

    Flow setSectionFlow(boolean sectionFlow);

    Flow setType(Type type);

    Flow setVariable(Variable variable);

    Flow addLineForSelectByCondition(Variable variable, Flow flow);

    Flow addLineForSelectByInlineCondition(String script, Flow flow);

    Flow setDefaultFlow(Flow flow);

    Flow setDefaultError(boolean defaultError);

    Flow setLocation(String location);

    enum Type {
        SIMPLE,
        SELECT_BY_INTEGER,
        SELECT_BY_INTERVAL,
        SELECT_BY_CONDITION,
        SELECT_BY_STRING,
        SELECT_BY_TEXT,
        SELECT_BY_INLINE_CONDITION,
        REPEATED,
        EXTERNAL,
        FIRST_FITTING,
        FIRST_FITTING_AUTO,
        VARIABLE_FORMATTED,
        OVERFLOWABLE_VARIABLE_FORMATTED,
        SELECT_BY_CONTENT,
        SELECT_BY_LANGUAGE,
        DIRECT_EXTERNAL,
        DYNAMIC_EXTERNAL,
    }
}