package com.quadient.wfdxml.api.layoutnodes.tables;

import com.quadient.wfdxml.api.Node;

public interface RowSet extends Node<RowSet> {

    enum Type {
        SINGLE_ROW,
        MULTIPLE_ROWS,
        REPEATED,
        SELECT_BY_INTEGER,
        SELECT_BY_INTERVAL,
        SELECT_BY_CONDITION,
        HEADER_AND_FOOTER,
        SELECT_BY_TEXT,
        SELECT_BY_INLINE_CONDITION,
    }
}