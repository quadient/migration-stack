package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.BinaryOrGroup
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.LiteralOrFunctionCall
import com.quadient.migration.shared.MetadataPrimitive

class DisplayRuleBuilder(id: String) : DtoBuilderBase<DisplayRule, DisplayRuleBuilder>(id) {
    var definition: DisplayRuleDefinition? = null
    var subject: String? = null
    var internal = true
    var targetId: String? = null
    var metadata: MutableMap<String, List<MetadataPrimitive>> = mutableMapOf()
    var variableStructureRef: VariableStructureRef? = null
    var baseTemplate: String? = null
    var targetFolder: String? = null

    /**
     * Sets the definition for this display rule.
     * This can be a complex group of binary expressions or a single binary expression.
     * @param definition The definition to set.
     * @return The current instance of DisplayRuleBuilder for method chaining.
     */
    fun definition(definition: DisplayRuleDefinition) = apply { this.definition = definition }

    /**
     * Sets a binary expression as the definition of this display rule.
     * This is a convenience method for creating a simple display rule with a single binary expression.
     * @param builder A builder function to build the binary expression.
     * @return The current instance of DisplayRuleBuilder for method chaining.
     */
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

    /**
     * Sets a group of display rules as the definition of this display rule.
     * This allows for complex conditions using logical operators.
     * @param builder A builder function to build the display rule group.
     * @return The current instance of DisplayRuleBuilder for method chaining.
     */
    fun group(builder: DisplayRuleGroupBuilder.() -> Unit) = apply {
        val group = DisplayRuleGroupBuilder().apply(builder).build()
        this.definition = DisplayRuleDefinition(group = group)
    }


    /**
     * Sets the subject of the display rule. This is visible as description in Interactive
     * @param subject the subject of the display rule
     * @return the builder instance for chaining
     */
    fun subject(value: String?) = apply { subject = value }

    /**
     * Set whether the display rule is internal. Internal objects do not create a separate
     * file in the target system.
     * @param internal Boolean indicating if the display rule is internal.
     * @return This builder instance for method chaining.
    */
    fun internal(value: Boolean) = apply { internal = value }

    /**
     * Sets the target display rule ID for alias resolution.
     * When set, this display rule reference will resolve to the specified display rule.
     * @param id the ID of the display rule to resolve to
     * @return the builder instance for chaining
     */
    fun targetId(id: String?) = apply { targetId = id }

    /**
     * Sets the target display rule for alias resolution.
     * When set, this display rule reference will resolve to the specified display rule.
     * @param rule The [DisplayRule] object to resolve to.
     * @return the builder instance for chaining
     */
    fun targetId(rule: DisplayRule) = apply { targetId = rule.id }

    /**
     * Sets the variable structure reference for this display rule.
     * @param value The [VariableStructureRef] to set.
     * @return This builder instance for method chaining.
     */
    fun variableStructureRef(value: VariableStructureRef) = apply { variableStructureRef = value }

    /**
     * Sets the variable structure reference for this display rule by ID.
     * @param value ID of the variable structure to reference.
     * @return This builder instance for method chaining.
     */
    fun variableStructureRef(value: String) = apply { variableStructureRef = VariableStructureRef(value) }

    /**
     * Sets the variable structure reference for this display rule from a [VariableStructure] object.
     * @param value The [VariableStructure] whose ID will be used as the reference.
     * @return This builder instance for method chaining.
     */
    fun variableStructureRef(value: VariableStructure) = apply { variableStructureRef = VariableStructureRef(value.id) }

    /**
     * Override the default base template for this display rule.
     * @param baseTemplate Path to the base template to use for this display rule.
     * @return This builder instance for method chaining.
     */
    fun baseTemplate(value: String?) = apply { baseTemplate = value }

    /**
     * Set the target folder for the display rule.
     * @param targetFolder String representing the target folder path.
     * @return This builder instance for method chaining.
     */
    fun targetFolder(value: String?) = apply { targetFolder = value }

    /**
     * Add metadata to the document object.
     * Metadata are not stored if empty.
     * @param key Key of the metadata entry.
     * @param block Builder function where receiver is a [MetadataBuilder].
     * @return This builder instance for method chaining.
     */
    fun metadata(key: String, block: MetadataBuilder.() -> Unit) = apply {
        val result = MetadataBuilder().apply(block).build()
        if (result.isNotEmpty()) {
            metadata[key] = result
        }
    }

    /**
     * Builds the DisplayRule instance with the provided properties.
     * @return A DisplayRule instance with the specified id, name, origin locations, custom fields, and definition.
     */
    override fun build(): DisplayRule {
        return DisplayRule(
            id = id,
            name = name,
            internal = internal,
            subject = subject,
            originLocations = originLocations,
            customFields = customFields,
            definition = definition,
            targetId = targetId?.let { DisplayRuleRef(it) },
            variableStructureRef = variableStructureRef,
            baseTemplate = baseTemplate,
            targetFolder = targetFolder,
            metadata = metadata,
        )
    }
}

class DisplayRuleGroupBuilder() {
    private var operator: GroupOp = GroupOp.And
    private var negation: Boolean = false
    private val items = mutableListOf<BinaryOrGroup>()

    /**
     * Adds a binary expression to the group.
     * This allows for creating complex conditions by combining multiple binary expressions.
     * @param builder A builder function to build the binary expression.
     * @return The current instance of DisplayRuleGroupBuilder for method chaining.
     */
    fun comparison(builder: BinaryExpressionBuilder.() -> Unit) = apply {
        val binaryExpression = BinaryExpressionBuilder().apply(builder).build()
        items.add(binaryExpression)
    }

    /**
     * Adds a group of display rules to the current group with logical AND operator.
     * Each group can only have one operator, this method replaces any previously set operator
     * for this group.
     * @param builder A builder function to build the nested display rule group.
     * @return The current instance of DisplayRuleGroupBuilder for method chaining.
     */
    fun and(builder: DisplayRuleGroupBuilder.() -> Unit) = apply {
        val group = DisplayRuleGroupBuilder().apply(builder)
        group.operator = GroupOp.And
        items.add(group.build())
    }

    /**
     * Adds a group of display rules to the current group with logical OR operator.
     * Each group can only have one operator, this method replaces any previously set operator
     * for this group.
     * @param builder A builder function to build the nested display rule group.
     * @return The current instance of DisplayRuleGroupBuilder for method chaining.
     */
    fun or(builder: DisplayRuleGroupBuilder.() -> Unit) = apply {
        val group = DisplayRuleGroupBuilder().apply(builder)
        group.operator = GroupOp.Or
        items.add(group.build())
    }

    /**
     * Sets the operator for the group.
     * Each group can only have one operator, this method replaces any previously set operator
     * for this group.
     * @param operator The logical operator to set for the group.
     * @return The current instance of DisplayRuleGroupBuilder for method chaining.
     */
    fun operator(operator: GroupOp) = apply { this.operator = operator }

    /**
     * Logically negates this group, meaning the group result will be inverted.
     * @return The current instance of DisplayRuleGroupBuilder for method chaining.
     */
    fun negate() = apply { this.negation = true }

    /**
     * Builds the DisplayRuleGroup instance with the provided items, operator, and negation.
     * @return A Group instance representing the display rule group.
     */
    fun build() = Group(items = items, operator = operator, negation = negation)
}

class BinaryExpressionBuilder() {
    var left: LiteralOrFunctionCall? = null
    var right: LiteralOrFunctionCall? = null
    var operator: BinOp? = null
    private var leftSet = false

    /**
     * Sets the next operand of the binary expression to a literal [Double] value.
     * The first call sets the left operand; the second call sets the right operand.
     * @param value The value to set as the next operand.
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun value(value: Double) = apply { setValue(value.toString(), LiteralDataType.Number) }

    /**
     * Sets the next operand of the binary expression to a literal [String] value.
     * The first call sets the left operand; the second call sets the right operand.
     * @param value The value to set as the next operand.
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun value(value: String) = apply { setValue(value, LiteralDataType.String) }

    /**
     * Sets the next operand of the binary expression to a literal [Boolean] value.
     * The first call sets the left operand; the second call sets the right operand.
     * @param value The value to set as the next operand.
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun value(value: Boolean) = apply { setValue(value.toString(), LiteralDataType.Boolean) }

    /**
     * Sets the next operand of the binary expression to a [LiteralOrFunctionCall] value.
     * The first call sets the left operand; the second call sets the right operand.
     * @param value The value to set as the next operand.
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun value(value: LiteralOrFunctionCall) = apply {
        setValue(value)
    }

    /**
     * Sets the next operand of the binary expression to a variable.
     * The first call sets the left operand; the second call sets the right operand.
     * @param variableName The name of the variable to set as the next operand.
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun variable(variableName: String) = apply { setValue(variableName, LiteralDataType.Variable) }

    /**
     * Sets the next operand of the binary expression to a variable.
     * The first call sets the left operand; the second call sets the right operand.
     * @param variable The [Variable] object to use as the next operand.
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun variable(variable: Variable) = apply { setValue(variable.id, LiteralDataType.Variable) }

    private fun setValue(value: LiteralOrFunctionCall) {
        if (leftSet) {
            right = value
        } else {
            left = value
            leftSet = true
        }
    }

    private fun setValue(value: String, dataType: LiteralDataType) {
        setValue(Literal(value, dataType))
    }

    /**
     * Sets the operator for this expression to [BinOp.Equals]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun equals() = apply { this.operator = BinOp.Equals }

    /**
     * Sets the operator for this expression to [BinOp.EqualsCaseInsensitive]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun equalsCaseInsensitive() = apply {
        this.operator = BinOp.EqualsCaseInsensitive
    }

    /**
     * Sets the operator for this expression to [BinOp.NotEquals]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun notEquals() = apply { this.operator = BinOp.NotEquals }

    /**
     * Sets the operator for this expression to [BinOp.NotEqualsCaseInsensitive]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun notEqualsCaseInsensitive() = apply {
        this.operator = BinOp.NotEqualsCaseInsensitive
    }

    /**
     * Sets the operator for this expression to [BinOp.GreaterThan]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun greaterThan() = apply { this.operator = BinOp.GreaterThan }

    /**
     * Sets the operator for this expression to [BinOp.GreaterOrEqualThan]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun greaterOrEqualThan() = apply { this.operator = BinOp.GreaterOrEqualThan }

    /**
     * Sets the operator for this expression to [BinOp.LessThan]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun lessThan() = apply { this.operator = BinOp.LessThan }

    /**
     * Sets the operator for this expression to [BinOp.LessOrEqualThen]
     * @return The current instance of BinaryExpressionBuilder for method chaining.
     */
    fun lessOrEqualThan() = apply { this.operator = BinOp.LessOrEqualThen }

    /**
     * Builds the BinaryExpression instance with the provided left and right values,
     * and an operator.
     * @return A BinaryOrGroup instance representing the binary expression.
     */
    fun build() : BinaryOrGroup {
        return Binary(
            left = requireNotNull(left),
            right = requireNotNull(right),
            operator = requireNotNull(operator)
        )
    }
}
