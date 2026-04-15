package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.ColumnBalancingType
import com.quadient.migration.shared.Size

class ColumnLayoutBuilder {
    private var numberOfColumns: Int = 2
    private var gutterWidth: Size = Size.ofMillimeters(0)
    private var balancingType: ColumnBalancingType = ColumnBalancingType.FirstColumn
    private var applyTo: ColumnApplyTo = ColumnApplyTo.ThisBlockOnly

    /**
     * Sets the number of columns. Defaults to 2 if not set.
     * @param numberOfColumns Number of columns (must be >= 1).
     * @return The [ColumnLayoutBuilder] instance for method chaining.
     */
    fun numberOfColumns(numberOfColumns: Int) = apply { this.numberOfColumns = numberOfColumns }

    /**
     * Sets the spacing between columns. Defaults to 0 if not set.
     * @param gutterWidth The gutter (inter-column spacing) as a [Size].
     * @return The [ColumnLayoutBuilder] instance for method chaining.
     */
    fun gutterWidth(gutterWidth: Size) = apply { this.gutterWidth = gutterWidth }

    /**
     * Sets the column balancing mode. Defaults to [ColumnBalancingType.FirstColumn] if not set.
     * @param balancingType The [ColumnBalancingType] to apply.
     * @return The [ColumnLayoutBuilder] instance for method chaining.
     */
    fun balancingType(balancingType: ColumnBalancingType) = apply { this.balancingType = balancingType }

    /**
     * Sets the scope of the column layout. Defaults to [ColumnApplyTo.ThisBlockOnly] if not set.
     * @param applyTo The [ColumnApplyTo] scope (whole template or this block only).
     * @return The [ColumnLayoutBuilder] instance for method chaining.
     */
    fun applyTo(applyTo: ColumnApplyTo) = apply { this.applyTo = applyTo }

    /**
     * Builds the [ColumnLayout] instance.
     * @return The constructed [ColumnLayout] instance.
     */
    fun build(): ColumnLayout = ColumnLayout(
        numberOfColumns = numberOfColumns,
        gutterWidth = gutterWidth,
        balancingType = balancingType,
        applyTo = applyTo,
    )
}
