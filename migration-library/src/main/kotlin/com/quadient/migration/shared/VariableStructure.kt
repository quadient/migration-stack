package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class VariablePathData(
    var path: VariablePath,
    var name: String? = null,
) {
    constructor(path: String, name: String? = null) : this(LiteralPath(path), name)
}