package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class DataType {
    DateTime, Integer, Integer64, Double, String, Boolean, Currency, Array, SubTree;

    fun toInteractiveDataType(): String {
        return when (this) {
            DateTime -> "DateTime"
            Integer -> "Int"
            Integer64 -> "Int64"
            Double -> "Double"
            String -> "String"
            Boolean -> "Bool"
            Currency -> "Currency"
            // TODO d.svitak
        }
    }
}