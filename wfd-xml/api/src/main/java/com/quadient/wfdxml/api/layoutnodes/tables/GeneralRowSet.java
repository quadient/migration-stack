package com.quadient.wfdxml.api.layoutnodes.tables;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface GeneralRowSet extends RowSet {

    GeneralRowSet setType(RowSet.Type rowSetType);

    GeneralRowSet addCell(Cell cell);

    GeneralRowSet addEmptyCell();

    GeneralRowSet addRowSet(RowSet rowSet);

    GeneralRowSet addEmptyRowSet();

    GeneralRowSet setVariable(Variable variable);

    GeneralRowSet setRowSet(RowSet rowSet);

    GeneralRowSet addLineForSelectByCondition(Variable variable, RowSet rowSet);

    GeneralRowSet addLineForSelectByInlineCondition(String script, RowSet rowSet);

    GeneralRowSet setDefaultRowSet(RowSet rowSet);

    GeneralRowSet setDefaultError(boolean defaultError);
}

