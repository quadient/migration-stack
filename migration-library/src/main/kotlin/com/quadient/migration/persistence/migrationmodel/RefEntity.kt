package com.quadient.migration.persistence.migrationmodel

import kotlinx.serialization.Serializable

@Serializable
sealed interface RefEntity

@Serializable
sealed interface TextContentEntity

@Serializable
sealed interface TextStyleDefOrRefEntity

@Serializable
sealed interface ParagraphStyleDefOrRefEntity

@Serializable
data class DocumentObjectEntityRef(val id: String, val displayRuleRef: DisplayRuleEntityRef? = null) : RefEntity,
    DocumentContentEntity, TextContentEntity

@Serializable
data class VariableEntityRef(val id: String) : RefEntity, TextContentEntity

@Serializable
data class TextStyleEntityRef(val id: String) : RefEntity, TextStyleDefOrRefEntity

@Serializable
data class ParagraphStyleEntityRef(val id: String) : RefEntity, ParagraphStyleDefOrRefEntity

@Serializable
data class DisplayRuleEntityRef(val id: String)

@Serializable
data class ImageEntityRef(val id: String) : RefEntity, DocumentContentEntity, TextContentEntity

@Serializable
data class VariableStructureEntityRef(val id: String) : RefEntity

@Serializable
data class StringEntity(val value: String) : TextContentEntity

@Serializable
data class FirstMatchEntity(val cases: List<CaseEntity>, val default: List<DocumentContentEntity>) :
    DocumentContentEntity, TextContentEntity {
    @Serializable
    data class CaseEntity(
        val displayRuleRef: DisplayRuleEntityRef, val content: List<DocumentContentEntity>, val name: String? = null
    )
}
