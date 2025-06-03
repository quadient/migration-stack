package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class SuperOrSubscript {
    None, Superscript, Subscript;
}