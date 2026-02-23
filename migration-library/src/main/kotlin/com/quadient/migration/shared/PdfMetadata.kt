package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class PdfMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: String? = null,
    val producer: String? = null,
)
