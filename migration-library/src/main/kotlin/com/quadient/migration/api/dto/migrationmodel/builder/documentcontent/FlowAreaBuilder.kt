package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.FlowArea
import com.quadient.migration.api.dto.migrationmodel.builder.PositionBuilder
import com.quadient.migration.shared.Position

class FlowAreaBuilder {
    private var content = mutableListOf<DocumentContent>()
    private var position: Position? = null

    fun content(content: List<DocumentContent>) = apply { this.content = content.toMutableList() }
    fun content(content: DocumentContent) = apply { this.content.add(content) }
    fun appendContent(content: DocumentContent) = apply { this.content.add(content) }
    fun appendContent(content: List<DocumentContent>) = apply { this.content.addAll(content) }
    fun position(position: Position) = apply { this.position = position }
    fun position(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        this.position = position
    }

    fun build(): FlowArea {
        return FlowArea(
            content = content,
            position = position ?: throw RuntimeException("Position is required in flow area")
        )
    }
}