package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class PdfTaggingRule {
    None,
    Default,
    Table
}

