package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class TableAlignment {
    Left,
    Center,
    Right,
    Inherit
}

@Serializable
enum class CellAlignment {
    Top,
    Center,
    Bottom,
}

@Serializable
sealed interface CellHeight {
    @Serializable
    data class Fixed(val size: Size) : CellHeight
    @Serializable
    data class Custom(val minHeight: Size, val maxHeight: Size): CellHeight
}
