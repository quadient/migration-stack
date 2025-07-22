package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class Alignment {
    Left, Right, Center, JustifyLeft, JustifyRight, JustifyCenter, JustifyBlock, JustifyWithMargins, JustifyBlockUniform
}

@Serializable
sealed class LineSpacing {
    @Serializable
    data class Additional(val size: Size?) : LineSpacing()

    @Serializable
    data class Exact(val size: Size?) : LineSpacing()

    @Serializable
    data class AtLeast(val size: Size?) : LineSpacing()

    @Serializable
    data class MultipleOf(val value: Double?) : LineSpacing()

    @Serializable
    data class ExactFromPreviousWithAdjustLegacy(val size: Size?) : LineSpacing()

    @Serializable
    data class ExactFromPreviousWithAdjust(val size: Size?) : LineSpacing()

    @Serializable
    data class ExactFromPrevious(val size: Size?) : LineSpacing()
}

@Serializable
enum class TabType {
    Left, Right, Center, DecimalWord, Decimal
}
