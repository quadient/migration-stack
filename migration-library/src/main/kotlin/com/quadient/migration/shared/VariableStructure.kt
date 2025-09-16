package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class VariablePathData(
    val path: String,
    val name: String? = null,
)