package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.PdfMetadata

class PdfMetadataBuilder {
    private var title: String? = null
    private var author: String? = null
    private var subject: String? = null
    private var keywords: String? = null
    private var producer: String? = null

    fun title(title: String) = apply { this.title = title }
    fun author(author: String) = apply { this.author = author }
    fun subject(subject: String) = apply { this.subject = subject }
    fun keywords(keywords: String) = apply { this.keywords = keywords }
    fun producer(producer: String) = apply { this.producer = producer }

    fun build(): PdfMetadata = PdfMetadata(
        title = title,
        author = author,
        subject = subject,
        keywords = keywords,
        producer = producer,
    )
}
