package com.quadient.wfdxml.api.layoutnodes.flow;

import com.quadient.wfdxml.api.layoutnodes.Element;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.Image;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
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

    Paragraph back();
}