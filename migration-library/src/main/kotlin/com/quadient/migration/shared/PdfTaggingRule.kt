package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class TablePdfTaggingRule {
    None,
    Default,
    Table,
    Artifact
}

@Serializable
enum class ParagraphPdfTaggingRule {
    Paragraph,
    Heading,
    Heading1,
    Heading2,
    Heading3,
    Heading4,
    Heading5,
    Heading6
}

