package com.quadient.wfdxml.api.layoutnodes.flow;

import com.quadient.wfdxml.api.layoutnodes.*;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid;
import com.quadient.wfdxml.api.layoutnodes.tables.Table;

public interface Text {

    Text setTextStyle(TextStyle textStyle);

    Text setExistingTextStyle(String id);

    Text appendText(String text);

    Text appendFlow(Flow flow);

    Text appendVariable(Variable variable);

    Text appendTable(Table table);

    Text appendImage(Image image);

    Text appendElement(Element element);

    Text appendSection(Section section);

    Text appendEmailComponentGrid(EmailComponentGrid grid);

    Paragraph back();
}