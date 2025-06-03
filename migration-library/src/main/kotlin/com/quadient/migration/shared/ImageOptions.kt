package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class ImageOptions(
    val resizeWidth: Size? = null, val resizeHeight: Size? = null
)