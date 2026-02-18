package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment
import kotlinx.serialization.Serializable
import com.quadient.migration.shared.TablePdfTaggingRule

@Serializable
sealed interface DocumentContentEntity

@Serializable
data class TableEntity(
    val rows: List<Row>,
    val header: List<Row> = emptyList(),
    val firstHeader: List<Row> = emptyList(),
    val footer: List<Row> = emptyList(),
    val lastFooter: List<Row> = emptyList(),
    val columnWidths: List<ColumnWidthEntity>,
    val pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default,
    val pdfAlternateText: String? = null,
    val minWidth: Size? = null,
    val maxWidth: Size? = null,
    val percentWidth: Double? = null,
    val border: BorderOptions? = null,
    val alignment: TableAlignment = TableAlignment.Left
) : DocumentContentEntity, TextContentEntity {
    @Serializable
    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleEntityRef?)

    @Serializable
    data class Cell(
        val content: List<DocumentContentEntity>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
        val height: CellHeight? = null,
        val border: BorderOptions? = null,
        val alignment: CellAlignment? = null,
    )

    @Serializable
    data class ColumnWidthEntity(val minWidth: Size, val percentWidth: Double)
}

@Serializable
data class ParagraphEntity(
    val content: MutableList<TextEntity>,
    val styleRef: ParagraphStyleEntityRef?,
    val displayRuleRef: DisplayRuleEntityRef?
) : DocumentContentEntity {
    @Serializable
    data class TextEntity(
        val content: MutableList<TextContentEntity>,
        val styleRef: TextStyleEntityRef?,
        val displayRuleRef: DisplayRuleEntityRef?
    )
}

@Serializable
data class AreaEntity(
    val content: List<DocumentContentEntity>, val position: Position?, val interactiveFlowName: String?, val flowToNextPage: Boolean = false
) : DocumentContentEntity
