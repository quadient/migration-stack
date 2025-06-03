package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.BinaryOrGroup
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType

class DisplayRuleBuilder(id: String) : DtoBuilderBase<DisplayRule, DisplayRuleBuilder>(id) {
    var definition: DisplayRuleDefinition? = null

    fun definition(definition: DisplayRuleDefinition) = apply { this.definition = definition }
    fun comparison(builder: BinaryExpressionBuilder.() -> Unit) = apply {
        val binaryExpression = BinaryExpressionBuilder().apply(builder).build()
        this.definition = DisplayRuleDefinition(
            group = Group(
                items = mutableListOf(binaryExpression),
                operator = GroupOp.And,
                negation = false
            )
        )
    }
    fun group(builder: DisplayRuleGroupBuilder.() -> Unit) = apply {
        val group = DisplayRuleGroupBuilder().apply(builder).build()
        this.definition = DisplayRuleDefinition(group = group)
    }

    override fun build(): DisplayRule {
        return DisplayRule(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            definition = definition,
        )
    }
}

class DisplayRuleGroupBuilder() {
    var operator: GroupOp? = null
    var negation: Boolean? = null
    val items = mutableListOf<BinaryOrGroup>()

    fun comparison(builder: BinaryExpressionBuilder.() -> Unit) = apply {
        val binaryExpression = BinaryExpressionBuilder().apply(builder).build()
        items.add(binaryExpression)
    }

    fun and(builder: DisplayRuleGroupBuilder.() -> Unit) = apply {
        val group = DisplayRuleGroupBuilder().apply(builder)
        group.operator = GroupOp.And
        items.add(group.build())
    }

    fun or(builder: DisplayRuleGroupBuilder.() -> Unit) = apply {
        val group = DisplayRuleGroupBuilder().apply(builder)
        group.operator = GroupOp.Or
        items.add(group.build())
    }

    fun negated() = apply {
        this.negation = true
    }

    fun build(): Group {
        return Group(
            items = items,
            operator = requireNotNull(operator),
            negation = requireNotNull(negation)
        )
    }
}

class BinaryExpressionBuilder() {
    var left: Literal? = null
    var right: Literal? = null
    var operator: BinOp? = null
    private var leftSet = false

    fun value(value: Double) = apply { setValue(value.toString(), LiteralDataType.Number) }
    fun value(value: String) = apply { setValue(value, LiteralDataType.String) }
    fun value(value: Boolean) = apply { setValue(value.toString(), LiteralDataType.Boolean) }
    fun variable(variableName: String) = apply { setValue(variableName, LiteralDataType.Variable) }

    private fun setValue(value: String, dataType: LiteralDataType) {
        if (leftSet) {
            right = Literal(value, dataType)
        } else {
            left = Literal(value, dataType)
            leftSet = true
        }
    }

    fun equals() = apply { this.operator = BinOp.Equals }
    fun notEquals() = apply { this.operator = BinOp.NotEquals }
    fun greaterThan() = apply { this.operator = BinOp.GreaterThan }
    fun greaterOrEqualThan() = apply { this.operator = BinOp.GreaterOrEqualThan }
    fun lessThan() = apply { this.operator = BinOp.LessThan }
    fun lessOrEqualThan() = apply { this.operator = BinOp.LessOrEqualThen }

    fun build() : BinaryOrGroup {
        return Binary(
            left = requireNotNull(left),
            right = requireNotNull(right),
            operator = requireNotNull(operator)
        )
    }
}
