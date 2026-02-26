package com.quadient.migration.persistence.migrationmodel

import kotlinx.serialization.Serializable

@Serializable
data class PdfMetadataEntity(
    val title: List<VariableStringContentEntity>? = null,
    val author: List<VariableStringContentEntity>? = null,
    val subject: List<VariableStringContentEntity>? = null,
    val keywords: List<VariableStringContentEntity>? = null,
    val producer: List<VariableStringContentEntity>? = null,
)
