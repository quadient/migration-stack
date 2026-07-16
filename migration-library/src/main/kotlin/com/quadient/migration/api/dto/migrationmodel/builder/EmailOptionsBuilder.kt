package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.EmailOptions
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.shared.Color

class EmailOptionsBuilder {
    private var width: Double? = null
    private var backgroundFill: Color = Color(255, 255, 255)
    private var from: List<VariableStringContent> = emptyList()
    private var fromName: List<VariableStringContent> = emptyList()
    private var subject: List<VariableStringContent> = emptyList()
    private var to: List<VariableStringContent> = emptyList()

    fun width(width: Double?) = apply { this.width = width }

    fun backgroundFill(color: Color) = apply { this.backgroundFill = color }
    fun backgroundFill(hex: String) = apply { this.backgroundFill = Color.fromHex(hex) }

    fun from(from: String) = apply { this.from = listOf(StringValue(from)) }
    fun from(vararg content: VariableStringContent) = apply { this.from = content.toList() }
    fun from(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.from = VariableStringContentBuilder().apply(builder).build() }

    fun fromName(fromName: String) = apply { this.fromName = listOf(StringValue(fromName)) }
    fun fromName(vararg content: VariableStringContent) = apply { this.fromName = content.toList() }
    fun fromName(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.fromName = VariableStringContentBuilder().apply(builder).build() }

    fun subject(subject: String) = apply { this.subject = listOf(StringValue(subject)) }
    fun subject(vararg content: VariableStringContent) = apply { this.subject = content.toList() }
    fun subject(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.subject = VariableStringContentBuilder().apply(builder).build() }

    fun to(to: String) = apply { this.to = listOf(StringValue(to)) }
    fun to(vararg content: VariableStringContent) = apply { this.to = content.toList() }
    fun to(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.to = VariableStringContentBuilder().apply(builder).build() }

    fun build(): EmailOptions = EmailOptions(
        width = width,
        backgroundFill = backgroundFill,
        from = from,
        fromName = fromName,
        subject = subject,
        to = to,
    )
}
