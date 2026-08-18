package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasPosition
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasTargetFolder
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasVariableStructureRef
import com.quadient.migration.shared.BaseTemplateArea
import com.quadient.migration.shared.BaseTemplatePage
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size

@DslMarker
annotation class BaseTemplateBuilderDsl

@BaseTemplateBuilderDsl
class BaseTemplateBuilder(id: String) : DtoBuilderBase<BaseTemplate, BaseTemplateBuilder>(id),
    HasTargetFolder<BaseTemplateBuilder>,
    HasVariableStructureRef<BaseTemplateBuilder> {
    override var targetFolder: String? = null
    override var variableStructureRef: VariableStructureRef? = null
    val pages = mutableListOf<Page>()

    /** Creates a new [Page], appends it, and returns it for further configuration. */
    fun addPage() = Page().also { pages.add(it) }

    /**
     * Creates a new [Page], configures it via [init], appends it, and returns this builder.
     * @return This builder instance for method chaining.
     */
    fun addPage(init: Page.() -> Unit): BaseTemplateBuilder = apply { pages.add(Page().apply(init)) }

    /**
     * Appends a pre-configured [page] to this base template.
     * @return This builder instance for method chaining.
     */
    fun addPage(page: Page) = apply { pages.add(page) }

    /**
     * Appends multiple pre-configured [pages] to this base template.
     * @return This builder instance for method chaining.
     */
    fun addPages(pages: List<Page>) = apply { this.pages.addAll(pages) }

    override fun build(): BaseTemplate {
        return BaseTemplate(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            targetFolder = targetFolder,
            pages = pages.map(Page::build),
            variableStructureRef = variableStructureRef,
        )
    }

    @BaseTemplateBuilderDsl
    class Page {
        var name: String? = null; private set
        var pageWidth: Size? = null; private set
        var pageHeight: Size? = null; private set
        val areas = mutableListOf<Area>()

        fun name(name: String?) = apply { this.name = name }
        fun pageWidth(pageWidth: Size?) = apply { this.pageWidth = pageWidth }
        fun pageHeight(pageHeight: Size?) = apply { this.pageHeight = pageHeight }

        /**
         * Sets both the width and height of the page.
         * @return This builder instance for method chaining.
         */
        fun pageSize(width: Size?, height: Size?) = apply {
            this.pageWidth = width
            this.pageHeight = height
        }

        /** Creates a new [Area], appends it, and returns it for further configuration. */
        fun addArea(interactiveFlowName: String) = Area(interactiveFlowName).also { areas.add(it) }

        /**
         * Creates a new [Area], configures it via [init], appends it, and returns this builder.
         * @return This builder instance for method chaining.
         */
        fun addArea(interactiveFlowName: String, init: Area.() -> Unit): Page =
            apply { areas.add(Area(interactiveFlowName).apply(init)) }

        /**
         * Appends a pre-configured [area] to this page.
         * @return This builder instance for method chaining.
         */
        fun addArea(area: Area) = apply { areas.add(area) }

        /**
         * Appends multiple pre-configured [areas] to this page.
         * @return This builder instance for method chaining.
         */
        fun addAreas(areas: List<Area>) = apply { this.areas.addAll(areas) }

        fun build(): BaseTemplatePage {
            return BaseTemplatePage(
                name = name,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                areas = areas.map(Area::build),
            )
        }
    }

    @BaseTemplateBuilderDsl
    class Area(val interactiveFlowName: String) : HasPosition<Area> {
        override var position: Position? = null
        var flowToNextPage: Boolean = false; private set

        /**
         * Set whether the flow area should flow to the next page.
         * @param flowToNextPage Whether the flow area should flow to the next page. Default is false.
         * @return The [Area] instance for method chaining.
         */
        fun flowToNextPage(flowToNextPage: Boolean) = apply { this.flowToNextPage = flowToNextPage }

        fun build(): BaseTemplateArea {
            return BaseTemplateArea(
                interactiveFlowName = interactiveFlowName,
                position = position,
                flowToNextPage = flowToNextPage,
            )
        }
    }
}
