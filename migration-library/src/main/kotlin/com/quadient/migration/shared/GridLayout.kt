package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class GridHorizontalAlignment {
    Left,
    Center,
    Right,
}

@Serializable
enum class GridAlignment {
    Top,
    Center,
    Bottom,
}

@Serializable
enum class OnMobile {
    FromLeft,
    FromRight,
    NoStacking,
}

@Serializable
enum class ColumnDistribution {
    EvenWidth,
    TwoColumns_25_75,
    TwoColumns_33_66,
    TwoColumns_66_33,
    TwoColumns_75_25,
    ThreeColumns_25_25_50,
    ThreeColumns_25_50_25,
    ThreeColumns_50_25_25,
}

