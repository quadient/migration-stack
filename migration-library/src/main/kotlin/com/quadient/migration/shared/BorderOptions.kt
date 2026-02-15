package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class BorderOptions(
    val leftLine: BorderLine? = null,
    val rightLine: BorderLine? = null,
    val topLine: BorderLine? = null,
    val bottomLine: BorderLine? = null,

    val paddingTop: Size = Size.ofMillimeters(0),
    val paddingBottom: Size = Size.ofMillimeters(0),
    val paddingLeft: Size = Size.ofMillimeters(0),
    val paddingRight: Size = Size.ofMillimeters(0),

    val fill: Color? = null,
)

@Serializable
data class BorderLine(val color: Color = Color(0, 0, 0), val width: Size = Size.ofMillimeters(0.2))