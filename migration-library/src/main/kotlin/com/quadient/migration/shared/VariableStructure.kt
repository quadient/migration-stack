package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class VariablePathAndName(
    val name: String?, val path: String
)