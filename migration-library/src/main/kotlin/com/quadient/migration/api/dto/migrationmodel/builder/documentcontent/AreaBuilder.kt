package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasPosition
import com.quadient.migration.shared.Position

class AreaBuilder : DocumentContentBuilderBase<AreaBuilder>, HasPosition<AreaBuilder> {
    override val content = mutableListOf<DocumentContent>()
    override var position: Position? = null
    private var interactiveFlowName: String? = null
    private var flowToNextPage: Boolean = false

    /**
     * Set the name of Interactive flow defined in Base Template to which the content will flow.
     * Main flow is used by default.
     * @param interactiveFlowName Name of the Interactive flow defined in Base Template.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun interactiveFlowName(interactiveFlowName: String) = apply { this.interactiveFlowName = interactiveFlowName }

    /**
     * Set whether the flow area should flow to the next page.
     * @param flowToNextPage Whether the flow area should flow to the next page. Default is false.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun flowToNextPage(flowToNextPage: Boolean) = apply { this.flowToNextPage = flowToNextPage }

    /**
     * Builds the [Area] instance.
     * @return The constructed [Area] instance.
     */
    fun build(): Area {
        return Area(
            content = content,
            position = position,
            interactiveFlowName = interactiveFlowName,
            flowToNextPage = flowToNextPage,
        )
    }
}