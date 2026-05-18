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
enum class CellOverflow {
    /** Overflow cell content to next page (default). Maps to {@code FlowToNextPage=True}. */
    OverflowContentToNextPage,

    /** Move cell to next page. Maps to {@code FlowToNextPage=False}. */
    MoveCellToNextPage,
}

@Serializable
sealed interface CellHeight {
    @Serializable
    data class Fixed(val size: Size) : CellHeight
    @Serializable
    data class Custom(val minHeight: Size, val maxHeight: Size): CellHeight
}
