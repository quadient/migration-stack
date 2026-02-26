package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.PdfMetadataEntity

data class PdfMetadata(
    val title: List<VariableStringContent>? = null,
    val author: List<VariableStringContent>? = null,
    val subject: List<VariableStringContent>? = null,
    val keywords: List<VariableStringContent>? = null,
    val producer: List<VariableStringContent>? = null,
) {
    fun toDb(): PdfMetadataEntity = PdfMetadataEntity(
        title = title?.map { it.toDb() },
        author = author?.map { it.toDb() },
        subject = subject?.map { it.toDb() },
        keywords = keywords?.map { it.toDb() },
        producer = producer?.map { it.toDb() },
    )

    companion object {
        fun fromDb(entity: PdfMetadataEntity): PdfMetadata = PdfMetadata(
            title = entity.title?.map { VariableStringContent.fromDb(it) },
            author = entity.author?.map { VariableStringContent.fromDb(it) },
            subject = entity.subject?.map { VariableStringContent.fromDb(it) },
            keywords = entity.keywords?.map { VariableStringContent.fromDb(it) },
            producer = entity.producer?.map { VariableStringContent.fromDb(it) },
        )
    }
}
