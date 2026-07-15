package com.quadient.wfdxml.api.module;

import com.quadient.wfdxml.api.layoutnodes.*;
import com.quadient.wfdxml.api.layoutnodes.data.Data;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentContent;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentPlaceHolder;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentRoot;
import com.quadient.wfdxml.api.layoutnodes.email.TMText;
import com.quadient.wfdxml.api.layoutnodes.SmsRoot;
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

    TableStyle addTableStyle();

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

    Root getRoot();

    SmsRoot addSmsRoot();

    SmsRoot getSmsRoot();

    EmailComponentRoot addEmailComponentRoot();

    EmailComponentGrid addEmailComponentGrid();

    EmailComponentContent addEmailComponentContent();

    EmailComponentPlaceHolder addEmailComponentPlaceHolder();

    TMText addEmailTMText();

    Section addSection();
}