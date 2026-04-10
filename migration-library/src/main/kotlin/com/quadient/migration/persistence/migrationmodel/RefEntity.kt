package com.quadient.migration.persistence.migrationmodel

import kotlinx.serialization.Serializable

@Serializable
sealed interface RefEntity

@Serializable
sealed interface TextContentEntity

@Serializable
sealed interface VariableStringContentEntity : DocumentContentEntity, TextContentEntity

@Serializable
data class DocumentObjectEntityRef(val id: String, val displayRuleRef: DisplayRuleEntityRef? = null) : RefEntity,
    DocumentContentEntity, TextContentEntity

@Serializable
data class VariableEntityRef(val id: String) : RefEntity, VariableStringContentEntity

@Serializable
data class TextStyleEntityRef(val id: String) : RefEntity

@Serializable
data class ParagraphStyleEntityRef(val id: String) : RefEntity

@Serializable
data class DisplayRuleEntityRef(val id: String) : RefEntity

@Serializable
sealed interface ResourceEntityRef : RefEntity, DocumentContentEntity, TextContentEntity

@Serializable
data class ImageEntityRef(val id: String) : ResourceEntityRef

@Serializable
data class AttachmentEntityRef(val id: String) : ResourceEntityRef

@Serializable
data class VariableStructureEntityRef(val id: String) : RefEntity

@Serializable
data class StringEntity(val value: String) : VariableStringContentEntity

@Serializable
data class FirstMatchEntity(val cases: List<CaseEntity>, val default: List<DocumentContentEntity>) :
    DocumentContentEntity, TextContentEntity {
    @Serializable
    data class CaseEntity(
        val displayRuleRef: DisplayRuleEntityRef, val content: List<DocumentContentEntity>, val name: String? = null
    )
}

@Serializable
data class SelectByLanguageEntity(val cases: List<CaseEntity>) : DocumentContentEntity {
    @Serializable
    data class CaseEntity(val language: String, val content: List<DocumentContentEntity>)
}
