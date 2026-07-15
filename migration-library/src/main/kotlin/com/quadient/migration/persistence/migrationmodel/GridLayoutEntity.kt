package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.Color
import com.quadient.migration.shared.ColumnDistribution
import com.quadient.migration.shared.GridAlignment
import com.quadient.migration.shared.GridHorizontalAlignment
import com.quadient.migration.shared.OnMobile
import kotlinx.serialization.Serializable

@Serializable
data class GridLayoutEntity(
    val columns: List<GridColumnEntity>,
    val distribution: ColumnDistribution,
    val verticalAlignment: GridAlignment,
    val columnStackingOnMobile: OnMobile,
    val paddingTop: Double,
    val paddingBottom: Double,
    val paddingLeft: Double,
    val paddingRight: Double,
    val fill: Color?,
    val fullWidthBackground: Boolean,
    val displayRuleRef: DisplayRuleEntityRef?,
) : DocumentContentEntity

@Serializable
sealed interface GridContentEntity {
    @Serializable
    data class Content(
        val content: List<DocumentContentEntity>,
        val paddingTop: Double = 0.0,
        val paddingBottom: Double = 0.0,
        val paddingLeft: Double = 0.0,
        val paddingRight: Double = 0.0,
    ) : GridContentEntity
    @Serializable
    data class Image(
        val ref: ImageEntityRef,
        val horizontalAlignment: GridHorizontalAlignment? = null,
        val width: Double? = null,
        val linkUrl: List<VariableStringContentEntity> = emptyList(),
        val openInNewWindow: Boolean = false,
        val paddingTop: Double = 0.0,
        val paddingBottom: Double = 0.0,
        val paddingLeft: Double = 0.0,
        val paddingRight: Double = 0.0,
    ) : GridContentEntity
    @Serializable
    data class ExternalImage(
        val url: List<VariableStringContentEntity>,
        val horizontalAlignment: GridHorizontalAlignment? = null,
        val width: Double? = null,
        val alternateText: String? = null,
        val linkUrl: List<VariableStringContentEntity> = emptyList(),
        val openInNewWindow: Boolean = false,
        val paddingTop: Double = 0.0,
        val paddingBottom: Double = 0.0,
        val paddingLeft: Double = 0.0,
        val paddingRight: Double = 0.0,
    ) : GridContentEntity
}

@Serializable
data class GridColumnEntity(val content: List<GridContentEntity>)
