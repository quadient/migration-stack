package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import kotlinx.serialization.Serializable

@Serializable
sealed interface DocumentContentEntity

@Serializable
data class TableEntity(
    val rows: List<Row>,
    val columnWidths: List<ColumnWidthEntity>,
) : DocumentContentEntity, TextContentEntity {
    @Serializable
    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleEntityRef?)

    @Serializable
    data class Cell(
        val content: List<DocumentContentEntity>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
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
    val content: List<DocumentContentEntity>, val position: Position?, val interactiveFlowName: String?
) : DocumentContentEntity
