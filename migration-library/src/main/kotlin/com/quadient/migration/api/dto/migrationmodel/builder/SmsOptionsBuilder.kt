package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.SmsOptions

class SmsOptionsBuilder {
    private var numberTo: List<VariableStringContent> = emptyList()

    fun numberTo(numberTo: String) = apply { this.numberTo = listOf(StringValue(numberTo)) }
    fun numberTo(vararg content: VariableStringContent) = apply { this.numberTo = content.toList() }
    fun numberTo(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.numberTo = VariableStringContentBuilder().apply(builder).build() }

    fun build(): SmsOptions = SmsOptions(numberTo = numberTo)
}
