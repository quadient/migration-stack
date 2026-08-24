package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.PdfMetadata

class PdfMetadataBuilder {
    var title: List<VariableStringContent>? = null; private set
    var author: List<VariableStringContent>? = null; private set
    var subject: List<VariableStringContent>? = null; private set
    var keywords: List<VariableStringContent>? = null; private set
    var producer: List<VariableStringContent>? = null; private set

    fun title(title: String) = apply { this.title = listOf(StringValue(title)) }
    fun author(author: String) = apply { this.author = listOf(StringValue(author)) }
    fun subject(subject: String) = apply { this.subject = listOf(StringValue(subject)) }
    fun keywords(keywords: String) = apply { this.keywords = listOf(StringValue(keywords)) }
    fun producer(producer: String) = apply { this.producer = listOf(StringValue(producer)) }

    fun title(title: Variable) = apply { this.title = listOf(VariableRef(title.id)) }
    fun author(author: Variable) = apply { this.author = listOf(VariableRef(author.id)) }
    fun subject(subject: Variable) = apply { this.subject = listOf(VariableRef(subject.id)) }
    fun keywords(keywords: Variable) = apply { this.keywords = listOf(VariableRef(keywords.id)) }
    fun producer(producer: Variable) = apply { this.producer = listOf(VariableRef(producer.id)) }

    fun title(title: VariableRef) = apply { this.title = listOf(title) }
    fun author(author: VariableRef) = apply { this.author = listOf(author) }
    fun subject(subject: VariableRef) = apply { this.subject = listOf(subject) }
    fun keywords(keywords: VariableRef) = apply { this.keywords = listOf(keywords) }
    fun producer(producer: VariableRef) = apply { this.producer = listOf(producer) }

    fun title(vararg content: VariableStringContent) = apply { this.title = content.toList() }
    fun author(vararg content: VariableStringContent) = apply { this.author = content.toList() }
    fun subject(vararg content: VariableStringContent) = apply { this.subject = content.toList() }
    fun keywords(vararg content: VariableStringContent) = apply { this.keywords = content.toList() }
    fun producer(vararg content: VariableStringContent) = apply { this.producer = content.toList() }

    fun title(content: List<VariableStringContent>) = apply { this.title = content.toList() }
    fun author(content: List<VariableStringContent>) = apply { this.author = content.toList() }
    fun subject(content: List<VariableStringContent>) = apply { this.subject = content.toList() }
    fun keywords(content: List<VariableStringContent>) = apply { this.keywords = content.toList() }
    fun producer(content: List<VariableStringContent>) = apply { this.producer = content.toList() }

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
