package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.shared.BorderLine
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size

class BorderOptionsBuilder {
    private var leftLine: BorderLine? = null
    private var rightLine: BorderLine? = null
    private var topLine: BorderLine? = null
    private var bottomLine: BorderLine? = null

    private var paddingTop: Size = Size.ofMillimeters(0)
    private var paddingBottom: Size = Size.ofMillimeters(0)
    private var paddingLeft: Size = Size.ofMillimeters(0)
    private var paddingRight: Size = Size.ofMillimeters(0)

    private var fill: Color? = null

    fun leftLine(color: Color?, width: Size?) = apply {
        val builder = BorderLineBuilder()
        color?.let { this.leftLine = this.leftLine?.copy(color = it) }
        width?.let { this.leftLine = this.leftLine?.copy(width = it) }
        builder.build()
    }
    fun rightLine(color: Color?, width: Size?) = apply {
        val builder = BorderLineBuilder()
        color?.let { this.rightLine = this.rightLine?.copy(color = it) }
        width?.let { this.rightLine = this.rightLine?.copy(width = it) }
        builder.build()
    }
    fun topLine(color: Color?, width: Size?) = apply {
        val builder = BorderLineBuilder()
        color?.let { this.topLine = this.topLine?.copy(color = it) }
        width?.let { this.topLine = this.topLine?.copy(width = it) }
        builder.build()
    }
    fun bottomLine(color: Color?, width: Size?) = apply {
        val builder = BorderLineBuilder()
        color?.let { this.bottomLine = this.bottomLine?.copy(color = it) }
        width?.let { this.bottomLine = this.bottomLine?.copy(width = it) }
        builder.build()
    }
    fun allBorders(color: Color?, width: Size?) = apply {
        val builder = BorderLineBuilder()
        color?.let { builder.color(it) }
        width?.let { builder.width(it) }
        val line = builder.build()
        this.leftLine = line
        this.rightLine = line
        this.topLine = line
        this.bottomLine = line
    }

    fun paddingTop(size: Size) = apply { this.paddingTop = size }
    fun paddingBottom(size: Size) = apply { this.paddingBottom = size }
    fun paddingLeft(size: Size) = apply { this.paddingLeft = size }
    fun paddingRight(size: Size) = apply { this.paddingRight = size }
    fun padding(all: Size) = apply {
        this.paddingTop = all
        this.paddingBottom = all
        this.paddingLeft = all
        this.paddingRight = all
    }

    fun fill(color: Color) = apply { this.fill = color }

    fun build() = BorderOptions(
        leftLine = leftLine,
        rightLine = rightLine,
        topLine = topLine,
        bottomLine = bottomLine,
        paddingTop = paddingTop,
        paddingBottom = paddingBottom,
        paddingLeft = paddingLeft,
        paddingRight = paddingRight,
        fill = fill
    )
}

class BorderLineBuilder {
    private var line = BorderLine()

    fun color(color: Color) = apply { line = line.copy(color = color) }
    fun width(size: Size) = apply { line = line.copy(width = size) }

    fun build() = line
}