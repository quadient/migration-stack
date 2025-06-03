package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class Position(val x: Size, val y: Size, val width: Size, val height: Size) {
    fun top() = y
    fun right() = x + width
    fun bottom() = y + height
    fun left() = x
}