package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.SmsOptions
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef

class SmsOptionsBuilder {
    var numberTo: List<VariableStringContent> = emptyList(); private set

    fun numberTo(numberTo: String) = apply { this.numberTo = listOf(StringValue(numberTo)) }
    fun numberTo(numberTo: Variable) = apply { this.numberTo = listOf(VariableRef(numberTo.id)) }
    fun numberTo(numberTo: VariableRef) = apply { this.numberTo = listOf(numberTo) }
    fun numberTo(vararg content: VariableStringContent) = apply { this.numberTo = content.toList() }
    fun numberTo(content: List<VariableStringContent>) = apply { this.numberTo = content.toList() }
    fun numberTo(builder: VariableStringContentBuilder.() -> Unit) =
        apply { this.numberTo = VariableStringContentBuilder().apply(builder).build() }

    fun build(): SmsOptions = SmsOptions(numberTo = numberTo)
}
