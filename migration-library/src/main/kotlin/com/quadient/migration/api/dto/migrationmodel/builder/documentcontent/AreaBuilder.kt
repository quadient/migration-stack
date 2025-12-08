package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase
import com.quadient.migration.api.dto.migrationmodel.builder.PositionBuilder
import com.quadient.migration.shared.Position

class AreaBuilder : DocumentContentBuilderBase<AreaBuilder> {
    override val content = mutableListOf<DocumentContent>()
    private var position: Position? = null
    private var interactiveFlowName: String? = null

    /**
     * Sets the position of the flow area.
     * @param position The [Position] to be set for the flow area.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun position(position: Position) = apply { this.position = position }

    /**
     * Set the position of the flow area using a builder function.
     * @param block a builder function to build the [Position].
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun position(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        this.position = position
    }

    /**
     * Set the name of Interactive flow defined in Base Template to which the content will flow.
     * Main flow is used by default.
     * @param interactiveFlowName Name of the Interactive flow defined in Base Template.
     * @return The [AreaBuilder] instance for method chaining.
     */
    fun interactiveFlowName(interactiveFlowName: String) = apply { this.interactiveFlowName = interactiveFlowName }

    /**
     * Builds the [Area] instance.
     * @return The constructed [Area] instance.
     */
    fun build(): Area {
        return Area(
            content = content,
            position = position,
            interactiveFlowName = interactiveFlowName,
        )
    }
}