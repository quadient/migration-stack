package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface Section extends Node<Section> {

    /**
     * Sets the number of columns.
     * Each column will have equal width. The gutter width applies between every adjacent pair of columns.
     *
     * @param numberOfColumns Number of columns (must be >= 1).
     * @return This instance for method chaining.
     */
    Section setNumberOfColumns(int numberOfColumns);

    /**
     * Sets the gutter width (spacing between columns) in meters.
     * Applied equally between all column pairs.
     *
     * @param gutterWidth Gutter width in meters.
     * @return This instance for method chaining.
     */
    Section setGutterWidth(double gutterWidth);

    /**
     * Sets the column balancing type.
     *
     * @param balancingType The {@link BalancingType} to apply.
     * @return This instance for method chaining.
     */
    Section setBalancingType(BalancingType balancingType);

    /**
     * Sets where the column layout is applied.
     *
     * @param applyTo The {@link ApplyTo} scope.
     * @return This instance for method chaining.
     */
    Section setApplyTo(ApplyTo applyTo);

    enum BalancingType {
        /** The first column takes more content than subsequent columns. */
        FIRST_COLUMN,
        /** All columns are balanced to equal height. */
        BALANCED,
        /** Columns are not balanced. */
        UNBALANCED
    }

    enum ApplyTo {
        /** Column layout applies to the whole template flow. */
        WHOLE_TEMPLATE,
        /** Column layout applies to this block only. */
        THIS_BLOCK_ONLY
    }
}
