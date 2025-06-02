package com.quadient.wfdxml.internal.layoutnodes.tables;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public class Column {
    public double minWidth = 0.0;
    public double percentWidth = 0.0;
    public Variable enableBy = null;
    public boolean header = false;

    public Column() {
    }

    public Column(double minWidth, double percentWidth, Variable enableBy, boolean header) {
        this.minWidth = minWidth;
        this.percentWidth = percentWidth;
        this.enableBy = enableBy;
        this.header = header;
    }
}
