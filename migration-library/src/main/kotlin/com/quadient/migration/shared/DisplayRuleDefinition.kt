package com.quadient.migration.shared

import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.RefValidatable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import kotlinx.serialization.Serializable

@Serializable
data class DisplayRuleDefinition(val group: Group) : RefValidatable {
    override fun collectRefs(): List<Ref> {
        return group.collectRefs()
    }
}

enum class GroupOp {
    And, Or;

    fun toInlineCondition(): String {
        return when (this) {
            And -> "and"
            Or -> "or"
        }
    }
}

enum class BinOp {
    Equals, EqualsCaseInsensitive, NotEquals, NotEqualsCaseInsensitive, GreaterThan, GreaterOrEqualThan, LessThan, LessOrEqualThen;
}

@Serializable
sealed class Function(val minArgs: Int, val maxArgs: Int, val args: List<LiteralOrFunctionCall>): LiteralOrFunctionCall {
    init {
        if (args.size !in minArgs..maxArgs) {
            error("Function $this requires between $minArgs and $maxArgs arguments, but got ${args.size}")
        }

        if (this.validate() != null) {
            error("Function $this is not valid: ${this.validate()}")
        }
    }

    override fun collectRefs(): List<Ref> {
        return args.flatMap { it.collectRefs() }
    }

    abstract fun validate(): String?

    @Serializable
    data class UpperCase(val arg: LiteralOrFunctionCall): Function(minArgs = 1, maxArgs = 1, args = listOf(arg)) {
        override fun validate(): String? {
            return null
        }
    }

    @Serializable
    data class LowerCase(val arg: LiteralOrFunctionCall): Function(minArgs = 1, maxArgs = 1, args = listOf(arg)) {
        override fun validate(): String? {
            return null
        }
    }
}

@Serializable
sealed class BinaryOrGroup : RefValidatable

@Serializable
sealed interface LiteralOrFunctionCall : RefValidatable

@Serializable
data class Binary(var left: LiteralOrFunctionCall, var operator: BinOp, var right: LiteralOrFunctionCall) : RefValidatable, BinaryOrGroup() {
    override fun collectRefs(): List<Ref> {
        return left.collectRefs() + right.collectRefs()
    }
}

@Serializable
data class Literal(var value: String, val dataType: LiteralDataType) : RefValidatable, LiteralOrFunctionCall {
    override fun collectRefs(): List<Ref> {
        return when (dataType) {
            LiteralDataType.Variable -> listOf(VariableRef(value))
            else -> emptyList()
        }
    }
}

@Serializable
enum class LiteralDataType {
    Variable, String, Number, Boolean
}

@Serializable
data class Group(val items: List<BinaryOrGroup>, val operator: GroupOp, val negation: Boolean) : RefValidatable,
    BinaryOrGroup() {

    override fun collectRefs(): List<Ref> {
        return items.flatMap {
            when (it) {
                is Group -> it.collectRefs()
                is Binary -> it.collectRefs()
            }
        }
    }
}