package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import kotlinx.serialization.Serializable

@Serializable
data class TextStyleDefinitionEntity(
    val fontFamily: String? = null,
    val foregroundColor: Color? = null,
    val size: Size? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val superOrSubscript: SuperOrSubscript,
    val interspacing: Size?,
) : TextStyleDefOrRefEntity

