package com.quadient.wfdxml.api.layoutnodes.tables;

public interface HeaderFooterRowSet extends RowSet {

    HeaderFooterRowSet setFirstHeader(RowSet rowset);

    HeaderFooterRowSet setHeader(RowSet rowset);

    HeaderFooterRowSet setBody(RowSet rowset);

    HeaderFooterRowSet setFooter(RowSet rowset);

    HeaderFooterRowSet setLastFooter(RowSet rowset);
}