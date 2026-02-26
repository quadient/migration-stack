package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.PdfMetadata

class PdfMetadataBuilder {
    private var title: List<VariableStringContent>? = null
    private var author: List<VariableStringContent>? = null
    private var subject: List<VariableStringContent>? = null
    private var keywords: List<VariableStringContent>? = null
    private var producer: List<VariableStringContent>? = null

    fun title(title: String) = apply { this.title = listOf(StringValue(title)) }
    fun author(author: String) = apply { this.author = listOf(StringValue(author)) }
    fun subject(subject: String) = apply { this.subject = listOf(StringValue(subject)) }
    fun keywords(keywords: String) = apply { this.keywords = listOf(StringValue(keywords)) }
    fun producer(producer: String) = apply { this.producer = listOf(StringValue(producer)) }

    fun title(vararg content: VariableStringContent) = apply { this.title = content.toList() }
    fun author(vararg content: VariableStringContent) = apply { this.author = content.toList() }
    fun subject(vararg content: VariableStringContent) = apply { this.subject = content.toList() }
    fun keywords(vararg content: VariableStringContent) = apply { this.keywords = content.toList() }
    fun producer(vararg content: VariableStringContent) = apply { this.producer = content.toList() }

    fun title(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.title = VariableStringContentBuilder().apply(builder).build() }

    fun author(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.author = VariableStringContentBuilder().apply(builder).build() }

    fun subject(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.subject = VariableStringContentBuilder().apply(builder).build() }

    fun keywords(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.keywords = VariableStringContentBuilder().apply(builder).build() }

    fun producer(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.producer = VariableStringContentBuilder().apply(builder).build() }

    fun build(): PdfMetadata = PdfMetadata(
        title = title,
        author = author,
        subject = subject,
        keywords = keywords,
        producer = producer,
    )
}
