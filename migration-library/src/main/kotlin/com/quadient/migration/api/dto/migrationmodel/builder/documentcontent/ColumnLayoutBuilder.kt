package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase
import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.ColumnBalancingType
import com.quadient.migration.shared.Size

class ColumnLayoutBuilder(private val numberOfColumns: Int) : DocumentContentBuilderBase<ColumnLayoutBuilder> {
    override val content = mutableListOf<DocumentContent>()
    private var gutterWidth: Size? = null
    private var balancingType: ColumnBalancingType? = null
    private var applyTo: ColumnApplyTo? = null

    /**
     * Sets the spacing between columns.
     * @param gutterWidth The gutter (inter-column spacing) as a [Size].
     * @return The [ColumnLayoutBuilder] instance for method chaining.
     */
    fun gutterWidth(gutterWidth: Size) = apply { this.gutterWidth = gutterWidth }

    /**
     * Sets the column balancing mode.
     * @param balancingType The [ColumnBalancingType] to apply.
     * @return The [ColumnLayoutBuilder] instance for method chaining.
     */
    fun balancingType(balancingType: ColumnBalancingType) = apply { this.balancingType = balancingType }

    /**
     * Sets the scope of the column layout.
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
        content = content,
    )
}
