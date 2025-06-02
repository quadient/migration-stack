package com.quadient.wfdxml.internal.layoutnodes.support;

import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public class PageCondition {
    public int integer;
    public Variable variable;
    public int end;
    public int rollback;
    public Page page;
    public String condStr;

    public PageCondition(int aBegin, int aEnd, int aRollback, Page aPage) {
        integer = aBegin;
        variable = null;
        end = aEnd;
        rollback = aRollback;
        page = aPage;
    }

    public PageCondition(Variable aVar, int aRollback, Page aPage) {
        integer = 0;
        variable = aVar;
        end = 0;
        rollback = aRollback;
        page = aPage;
    }

    public PageCondition(String aCondStr, int aRollback, Page aPage) {
        integer = 0;
        variable = null;
        end = 0;
        rollback = aRollback;
        page = aPage;
        condStr = aCondStr;
    }
}
