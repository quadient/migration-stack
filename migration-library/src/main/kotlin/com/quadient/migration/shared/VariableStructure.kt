package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class VariablePathData(
    var path: String,
    var name: String? = null,
)