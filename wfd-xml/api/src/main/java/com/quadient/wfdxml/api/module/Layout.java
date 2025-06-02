package com.quadient.wfdxml.api.module;

import com.quadient.wfdxml.api.layoutnodes.Color;
import com.quadient.wfdxml.api.layoutnodes.Element;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.Font;
import com.quadient.wfdxml.api.layoutnodes.Image;
import com.quadient.wfdxml.api.layoutnodes.LineStyle;
import com.quadient.wfdxml.api.layoutnodes.NumberedList;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.Pages;
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle;
import com.quadient.wfdxml.api.layoutnodes.Root;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Data;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.api.layoutnodes.tables.Cell;
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet;
import com.quadient.wfdxml.api.layoutnodes.tables.HeaderFooterRowSet;
import com.quadient.wfdxml.api.layoutnodes.tables.Table;

public interface Layout extends WorkFlowModule<Layout> {

    Color addColor();

    FillStyle addFillStyle();

    Font addFont();

    BorderStyle addBorderStyle();

    TextStyle addTextStyle();

    ParagraphStyle addParagraphStyle();

    ParagraphStyle addBulletParagraph(TextStyle textStyle, String bullet);

    ParagraphStyle addNumberingParagraph(TextStyle textStyle, String bullet_suffix, Variable variable);

    Flow addFlow();

    Table addTable();

    GeneralRowSet addRowSet();

    HeaderFooterRowSet addRowSetHeaderFooter();

    Cell addCell();

    Page addPage();

    Data getData();

    Pages getPages();

    NumberedList addNumberedList();

    Image addImage();

    LineStyle addLineStyle();

    Element addElement();

    Root addRoot();
}