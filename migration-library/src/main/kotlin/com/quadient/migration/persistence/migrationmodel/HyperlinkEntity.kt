package com.quadient.migration.persistence.migrationmodel

import kotlinx.serialization.Serializable

@Serializable
data class HyperlinkEntity(
    val url: String,
    val displayText: String? = null,
    val alternateText: String? = null
) : TextContentEntity
