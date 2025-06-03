package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class Alignment {
    Left, Right, Center, JustifyLeft, JustifyRight, JustifyCenter, JustifyBlock, JustifyWithMargins, JustifyBlockUniform
}

@Serializable
enum class LineSpacing {
    Additional, Exact, AtLeast, MultipleOf, ExactFromPreviousWithAdjustLegacy, ExactFromPreviousWithAdjust, ExactFromPrevious
}

@Serializable
enum class TabType {
    Left, Right, Center, DecimalWord, Decimal
}
