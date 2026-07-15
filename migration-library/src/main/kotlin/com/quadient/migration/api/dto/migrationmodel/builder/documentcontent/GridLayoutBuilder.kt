package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.GridContent
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStringContentBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDoublePadding
import com.quadient.migration.api.dto.migrationmodel.Column
import com.quadient.migration.api.dto.migrationmodel.GridLayout
import com.quadient.migration.api.dto.migrationmodel.builder.HasBarcodeContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasDocumentObjectRefContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasGridLayoutContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasImageRefContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasParagraphContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasRepeatedContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasStringContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasTableContent
import com.quadient.migration.api.dto.migrationmodel.builder.HasVariableRefContent
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.ColumnDistribution
import com.quadient.migration.shared.GridAlignment
import com.quadient.migration.shared.GridHorizontalAlignment
import com.quadient.migration.shared.OnMobile

class GridLayoutBuilder : HasDisplayRuleRef<GridLayoutBuilder> {
    private val columns = mutableListOf<Column>()
    private var distribution: ColumnDistribution = ColumnDistribution.EvenWidth
    private var verticalAlignment: GridAlignment = GridAlignment.Top
    private var columnStackingOnMobile: OnMobile = OnMobile.FromLeft
    private var paddingTop: Double = 0.0
    private var paddingBottom: Double = 0.0
    private var paddingLeft: Double = 0.0
    private var paddingRight: Double = 0.0
    private var fill: Color? = null
    private var fullWidthBackground: Boolean = false
    override var displayRuleRef: DisplayRuleRef? = null

    /**
     * Sets the column width distribution of the grid.
     * Defaults to [ColumnDistribution.EvenWidth] if not set.
     * @param distribution The [ColumnDistribution] to apply.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun distribution(distribution: ColumnDistribution) = apply { this.distribution = distribution }

    /**
     * Sets the vertical alignment of the grid columns.
     * Defaults to [GridAlignment.Top] if not set.
     * @param verticalAlignment The [GridAlignment] to apply.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun verticalAlignment(verticalAlignment: GridAlignment) = apply { this.verticalAlignment = verticalAlignment }

    /**
     * Sets the column stacking behavior on mobile devices.
     * Defaults to [OnMobile.FromLeft] if not set.
     * @param columnStackingOnMobile The [OnMobile] stacking strategy to apply.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun columnStackingOnMobile(columnStackingOnMobile: OnMobile) = apply { this.columnStackingOnMobile = columnStackingOnMobile }

    /**
     * Sets the top padding of the grid layout in pixels.
     * @param paddingTop The top padding in pixels.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun paddingTop(paddingTop: Double) = apply { this.paddingTop = paddingTop }

    /**
     * Sets the bottom padding of the grid layout in pixels.
     * @param paddingBottom The bottom padding in pixels.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun paddingBottom(paddingBottom: Double) = apply { this.paddingBottom = paddingBottom }

    /**
     * Sets the left padding of the grid layout in pixels.
     * @param paddingLeft The left padding in pixels.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun paddingLeft(paddingLeft: Double) = apply { this.paddingLeft = paddingLeft }

    /**
     * Sets the right padding of the grid layout in pixels.
     * @param paddingRight The right padding in pixels.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun paddingRight(paddingRight: Double) = apply { this.paddingRight = paddingRight }

    /**
     * Sets uniform padding on all sides of the grid layout in pixels.
     * @param padding The padding in pixels to apply to all sides.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun padding(padding: Double) = apply {
        this.paddingTop = padding
        this.paddingBottom = padding
        this.paddingLeft = padding
        this.paddingRight = padding
    }

    /**
     * Sets the background fill color of the grid layout.
     * @param fill The [Color] to use as the background fill, or null for no fill.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun fill(fill: Color?) = apply { this.fill = fill }

    /**
     * Sets whether the background should span the full width.
     * Defaults to false if not set.
     * @param fullWidthBackground Whether the background should be full width.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun fullWidthBackground(fullWidthBackground: Boolean) = apply { this.fullWidthBackground = fullWidthBackground }

    /**
     * Adds a column to the grid layout using a builder function.
     * @param builder A builder function to configure the column content.
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun column(builder: ColumnBuilder.() -> Unit) = apply {
        columns.add(ColumnBuilder().apply(builder).build())
    }

    /**
     * Adds a column with the given content to the grid layout.
     * @param content The list of [DocumentContent] for the column, wrapped as [GridContent.Content].
     * @return The [GridLayoutBuilder] instance for method chaining.
     */
    fun column(content: List<DocumentContent>) = apply {
        columns.add(Column(listOf(GridContent.Content(content))))
    }

    /**
     * Builds the [GridLayout] instance.
     * @return The constructed [GridLayout] instance.
     */
    fun build(): GridLayout {
        require(columns.isNotEmpty()) { "GridLayout must have at least one column" }
        require(columns.size <= 4) { "GridLayout supports at most 4 columns" }
        return GridLayout(
            columns = columns,
            distribution = distribution,
            verticalAlignment = verticalAlignment,
            columnStackingOnMobile = columnStackingOnMobile,
            paddingTop = paddingTop,
            paddingBottom = paddingBottom,
            paddingLeft = paddingLeft,
            paddingRight = paddingRight,
            fill = fill,
            fullWidthBackground = fullWidthBackground,
            displayRuleRef = displayRuleRef,
        )
    }

    class ColumnBuilder {
        private val gridContent = mutableListOf<GridContent>()

        /**
         * Adds a [GridContent.Content] block built with a [DocumentContentBuilderBase] DSL.
         * @param builder A builder function to configure the document content inside the block.
         * @return The [ColumnBuilder] instance for method chaining.
         */
        fun content(builder: ContentBuilder.() -> Unit): ColumnBuilder = apply {
            gridContent.add(ContentBuilder().apply(builder).build())
        }

        /**
         * Adds an image column content block.
         * @param builder A builder function to configure the image content.
         * @return The [ColumnBuilder] instance for method chaining.
         */
        fun image(builder: ImageBuilder.() -> Unit): ColumnBuilder = apply {
            ImageBuilder().apply(builder).build()?.let { gridContent.add(it) }
        }

        /**
         * Adds an external image column content block.
         * @param builder A builder function to configure the external image content.
         * @return The [ColumnBuilder] instance for method chaining.
         */
        fun externalImage(builder: ExternalImageBuilder.() -> Unit): ColumnBuilder = apply {
            gridContent.add(ExternalImageBuilder().apply(builder).build())
        }

        /**
         * Builds the [Column] instance.
         * @return The constructed [Column] instance.
         */
        fun build(): Column = Column(gridContent)

        class ContentBuilder :
            HasParagraphContent<ContentBuilder>,
            HasTableContent<DocumentContent, ContentBuilder>,
            HasImageRefContent<DocumentContent, ContentBuilder>,
            HasDocumentObjectRefContent<DocumentContent, ContentBuilder>,
            HasStringContent<DocumentContent, ContentBuilder>,
            HasVariableRefContent<DocumentContent, ContentBuilder>,
            HasRepeatedContent<ContentBuilder>,
            HasBarcodeContent<DocumentContent, ContentBuilder>,
            HasGridLayoutContent<DocumentContent, ContentBuilder>,
            HasDoublePadding<ContentBuilder> {
            override val content = mutableListOf<DocumentContent>()
            override var paddingTop: Double = 0.0
            override var paddingBottom: Double = 0.0
            override var paddingLeft: Double = 0.0
            override var paddingRight: Double = 0.0

            fun build(): GridContent.Content =
                GridContent.Content(content, paddingTop, paddingBottom, paddingLeft, paddingRight)
        }

        class ImageBuilder : HasDoublePadding<ImageBuilder> {
            internal var ref: ImageRef? = null
            var horizontalAlignment: GridHorizontalAlignment? = null
            var width: Double? = null
            internal var linkUrl: List<VariableStringContent> = emptyList()
            var openInNewWindow: Boolean = false
            override var paddingTop: Double = 0.0
            override var paddingBottom: Double = 0.0
            override var paddingLeft: Double = 0.0
            override var paddingRight: Double = 0.0

            fun imageRef(imageId: String) = apply { ref = ImageRef(imageId) }
            fun horizontalAlignment(horizontalAlignment: GridHorizontalAlignment) = apply { this.horizontalAlignment = horizontalAlignment }
            fun width(width: Double) = apply { this.width = width }
            fun linkUrl(linkUrl: String) = apply { this.linkUrl = listOf(StringValue(linkUrl)) }
            fun linkUrl(linkUrl: List<VariableStringContent>) = apply { this.linkUrl = linkUrl }
            fun linkUrl(builder: VariableStringContentBuilder.() -> Unit) = apply { this.linkUrl = VariableStringContentBuilder().apply(builder).build() }
            fun openInNewWindow(openInNewWindow: Boolean) = apply { this.openInNewWindow = openInNewWindow }

            fun build(): GridContent.Image? = ref?.let {
                GridContent.Image(
                    it,
                    horizontalAlignment,
                    width,
                    linkUrl,
                    openInNewWindow,
                    paddingTop,
                    paddingBottom,
                    paddingLeft,
                    paddingRight
                )
            }
        }

        class ExternalImageBuilder : HasDoublePadding<ExternalImageBuilder> {
            internal var url: List<VariableStringContent> = emptyList()
            var horizontalAlignment: GridHorizontalAlignment? = null
            var width: Double? = null
            var alternateText: String? = null
            internal var linkUrl: List<VariableStringContent> = emptyList()
            var openInNewWindow: Boolean = false
            override var paddingTop: Double = 0.0
            override var paddingBottom: Double = 0.0
            override var paddingLeft: Double = 0.0
            override var paddingRight: Double = 0.0

            fun url(url: String) = apply { this.url = listOf(StringValue(url)) }
            fun url(url: List<VariableStringContent>) = apply { this.url = url }
            fun url(builder: VariableStringContentBuilder.() -> Unit) = apply { this.url = VariableStringContentBuilder().apply(builder).build() }
            fun linkUrl(linkUrl: String) = apply { this.linkUrl = listOf(StringValue(linkUrl)) }
            fun linkUrl(linkUrl: List<VariableStringContent>) = apply { this.linkUrl = linkUrl }
            fun linkUrl(builder: VariableStringContentBuilder.() -> Unit) = apply { this.linkUrl = VariableStringContentBuilder().apply(builder).build() }
            fun horizontalAlignment(horizontalAlignment: GridHorizontalAlignment) = apply { this.horizontalAlignment = horizontalAlignment }
            fun width(width: Double) = apply { this.width = width }
            fun alternateText(alternateText: String) = apply { this.alternateText = alternateText }
            fun openInNewWindow(openInNewWindow: Boolean) = apply { this.openInNewWindow = openInNewWindow }

            fun build(): GridContent.ExternalImage =
                GridContent.ExternalImage(url, horizontalAlignment, width, alternateText, linkUrl, openInNewWindow, paddingTop, paddingBottom, paddingLeft, paddingRight)
        }
    }
}
