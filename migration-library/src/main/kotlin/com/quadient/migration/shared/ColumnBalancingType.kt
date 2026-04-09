package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class ColumnBalancingType {
    FirstColumn,
    Balanced,
    Unbalanced
}
