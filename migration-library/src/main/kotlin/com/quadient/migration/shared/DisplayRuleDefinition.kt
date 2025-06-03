package com.quadient.migration.shared

import com.quadient.migration.data.RefModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.service.RefValidatable
import kotlinx.serialization.Serializable

@Serializable
data class DisplayRuleDefinition(val group: Group) : RefValidatable {
    override fun collectRefs(): List<RefModel> {
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
    Equals, NotEquals, GreaterThan, GreaterOrEqualThan, LessThan, LessOrEqualThen, Add, Sub, Mul, Div;

    fun toInlineCondition(): String {
        return when (this) {
            Equals -> "=="
            NotEquals -> "!="
            GreaterThan -> ">"
            LessThan -> "<"
            GreaterOrEqualThan -> ">="
            LessOrEqualThen -> "<="
            Add -> TODO()
            Sub -> TODO()
            Mul -> TODO()
            Div -> TODO()
        }
    }
}

@Serializable
sealed class BinaryOrGroup : RefValidatable

@Serializable
data class Binary(val left: Literal, val operator: BinOp, val right: Literal) : RefValidatable, BinaryOrGroup() {
    override fun collectRefs(): List<RefModel> {
        return left.collectRefs() + right.collectRefs()
    }
}

@Serializable
data class Literal(var value: String, val dataType: LiteralDataType) : RefValidatable {
    override fun collectRefs(): List<RefModel> {
        return when (dataType) {
            LiteralDataType.Variable -> listOf(VariableModelRef(value))
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

    override fun collectRefs(): List<RefModel> {
        return items.flatMap {
            when (it) {
                is Group -> it.collectRefs()
                is Binary -> it.collectRefs()
            }
        }
    }
}
