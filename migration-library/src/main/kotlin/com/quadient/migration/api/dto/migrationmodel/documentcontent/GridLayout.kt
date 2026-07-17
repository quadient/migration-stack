package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.GridColumnEntity
import com.quadient.migration.persistence.migrationmodel.GridContentEntity
import com.quadient.migration.persistence.migrationmodel.GridLayoutEntity
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.ColumnDistribution
import com.quadient.migration.shared.GridAlignment
import com.quadient.migration.shared.GridHorizontalAlignment
import com.quadient.migration.shared.OnMobile

data class GridLayout(
    val columns: List<Column>, // 1-4
    val distribution: ColumnDistribution,
    val verticalAlignment: GridAlignment,
    val columnStackingOnMobile: OnMobile,
    val paddingTop: Double,
    val paddingBottom: Double,
    val paddingLeft: Double,
    val paddingRight: Double,
    val fill: Color?,
    val fullWidthBackground: Boolean,
    val displayRuleRef: DisplayRuleRef?,
) : DocumentContent, RefValidatable {
    override val pathName = "grid"

    override fun toPreview(nameResolver: (DocumentContent) -> String?) = "$pathName: ${columns.size} cols"

    companion object {
        fun fromDb(entity: GridLayoutEntity): GridLayout = GridLayout(
            columns = entity.columns.map { Column(it.content.map(GridContent::fromDb)) },
            distribution = entity.distribution,
            verticalAlignment = entity.verticalAlignment,
            paddingTop = entity.paddingTop,
            paddingBottom = entity.paddingBottom,
            paddingLeft = entity.paddingLeft,
            paddingRight = entity.paddingRight,
            fill = entity.fill,
            fullWidthBackground = entity.fullWidthBackground,
            displayRuleRef = entity.displayRuleRef?.let(DisplayRuleRef::fromDb),
            columnStackingOnMobile = entity.columnStackingOnMobile,
        )
    }

    fun toDb() = GridLayoutEntity(
        columns = columns.map { GridColumnEntity(it.content.map(GridContent::toDb)) },
        distribution = distribution,
        verticalAlignment = verticalAlignment,
        paddingTop = paddingTop,
        paddingBottom = paddingBottom,
        paddingLeft = paddingLeft,
        paddingRight = paddingRight,
        fill = fill,
        fullWidthBackground = fullWidthBackground,
        displayRuleRef = displayRuleRef?.toDb(),
        columnStackingOnMobile = columnStackingOnMobile,
    )

    override fun collectRefs(): List<Ref> {
        return listOfNotNull(this.displayRuleRef as? Ref) + this.columns.flatMap { column ->
            return column.content.flatMap { content ->
                return when (content) {
                    is GridContent.Content -> {
                        content.content.flatMap { (it as? RefValidatable)?.collectRefs() ?: emptyList() }
                    }

                    is GridContent.Image -> listOf(content.ref)
                    is GridContent.ExternalImage -> null
                } ?: emptyList()
            }
        }
    }
}

data class Column(val content: List<GridContent>)

sealed interface GridContent {
    val paddingTop: Double
    val paddingBottom: Double
    val paddingLeft: Double
    val paddingRight: Double
    fun toDb(): GridContentEntity

    data class Content(
        val content: List<DocumentContent>,
        override val paddingTop: Double = 0.0,
        override val paddingBottom: Double = 0.0,
        override val paddingLeft: Double = 0.0,
        override val paddingRight: Double = 0.0,
    ) : GridContent {
        override fun toDb() =
            GridContentEntity.Content(content.toDb(), paddingTop, paddingBottom, paddingLeft, paddingRight)

        companion object {
            fun fromDb(entity: GridContentEntity.Content): Content = Content(
                entity.content.map(DocumentContent::fromDbContent),
                entity.paddingTop,
                entity.paddingBottom,
                entity.paddingLeft,
                entity.paddingRight
            )
        }
    }

    data class Image(
        val ref: ImageRef,
        val horizontalAlignment: GridHorizontalAlignment? = null,
        val width: Double? = null,
        val linkUrl: List<VariableStringContent> = emptyList(),
        val openInNewWindow: Boolean = false,
        override val paddingTop: Double = 0.0,
        override val paddingBottom: Double = 0.0,
        override val paddingLeft: Double = 0.0,
        override val paddingRight: Double = 0.0,
    ) : GridContent {
        override fun toDb() = GridContentEntity.Image(ref.toDb(), horizontalAlignment, width, linkUrl.map { it.toDb() }, openInNewWindow, paddingTop, paddingBottom, paddingLeft, paddingRight)

        companion object {
            fun fromDb(entity: GridContentEntity.Image): Image = Image(
                ImageRef.fromDb(entity.ref),
                entity.horizontalAlignment,
                entity.width,
                entity.linkUrl.map(VariableStringContent::fromDb),
                entity.openInNewWindow,
                entity.paddingTop,
                entity.paddingBottom,
                entity.paddingLeft,
                entity.paddingRight
            )
        }
    }

    data class ExternalImage(
        val url: List<VariableStringContent>,
        val horizontalAlignment: GridHorizontalAlignment? = null,
        val width: Double? = null,
        val alternateText: String? = null,
        val linkUrl: List<VariableStringContent> = emptyList(),
        val openInNewWindow: Boolean = false,
        override val paddingTop: Double = 0.0,
        override val paddingBottom: Double = 0.0,
        override val paddingLeft: Double = 0.0,
        override val paddingRight: Double = 0.0,
    ) : GridContent {
        override fun toDb() = GridContentEntity.ExternalImage(url.map { it.toDb() }, horizontalAlignment, width, alternateText, linkUrl.map { it.toDb() }, openInNewWindow, paddingTop, paddingBottom, paddingLeft, paddingRight)

        companion object {
            fun fromDb(entity: GridContentEntity.ExternalImage): ExternalImage = ExternalImage(
                entity.url.map(VariableStringContent::fromDb),
                entity.horizontalAlignment,
                entity.width,
                entity.alternateText,
                entity.linkUrl.map(VariableStringContent::fromDb),
                entity.openInNewWindow,
                entity.paddingTop,
                entity.paddingBottom,
                entity.paddingLeft,
                entity.paddingRight
            )
        }
    }

    companion object {
        fun fromDb(entity: GridContentEntity): GridContent = when (entity) {
            is GridContentEntity.Content -> Content.fromDb(entity)
            is GridContentEntity.Image -> Image.fromDb(entity)
            is GridContentEntity.ExternalImage -> ExternalImage.fromDb(entity)
        }
    }
}