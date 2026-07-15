package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
sealed interface DocumentObjectOptions

@Serializable
data class PageOptions(
    val width: Size?,
    val height: Size?,
) : DocumentObjectOptions

@Serializable
data class EmailOptions(
    val width: Double?,
    val backgroundFill: Color,
) : DocumentObjectOptions
