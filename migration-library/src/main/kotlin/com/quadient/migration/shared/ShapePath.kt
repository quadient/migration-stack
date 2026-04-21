package com.quadient.migration.shared;

import kotlinx.serialization.Serializable

@Serializable
sealed interface ShapePath {
    @Serializable
    data class MoveTo(val x: Size, val y: Size) : ShapePath
    @Serializable
    data class LineTo(val x: Size, val y: Size) : ShapePath
    @Serializable
    data class ConicTo(val x0: Size, val y0: Size, val x1: Size, val y1: Size) : ShapePath
    @Serializable
    data class BezierTo(val x0: Size, val y0: Size, val x1: Size, val y1: Size, val x2: Size, val y2: Size) : ShapePath
}
