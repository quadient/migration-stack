package com.quadient.migration.shared

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.ser.std.StdSerializer
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.service.inspirebuilder.ArrayVariable
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.inspirebuilder.SubtreeVariable
import com.quadient.migration.service.inspirebuilder.VariablePathPart
import com.quadient.migration.service.inspirebuilder.buildVariableTree
import com.quadient.migration.service.inspirebuilder.removeDataFromVariablePath
import com.quadient.migration.service.inspirebuilder.toScript
import com.quadient.migration.service.inspirebuilder.resolve
import com.quadient.migration.service.inspirebuilder.variableToScript
import kotlinx.serialization.Serializable

@Serializable
class Jrd(@field:JsonProperty("InteractivePlusJsonDefinition") val interactivePlusJsonDefinition: JrdDefinition) {
    companion object {
        fun fromDisplayRule(
            rule: DisplayRule,
            projectConfig: ProjectConfig,
            variableStructure: VariableStructure,
            findVar: (String) -> Variable
        ): String {
            val result = Jrd(JrdDefinition.fromDisplayRule(rule, projectConfig, variableStructure, findVar))
            return JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build()
                .writerWithDefaultPrettyPrinter().writeValueAsString(result)
        }
    }
}

@Serializable
data class JrdDefinition(
    @field:JsonProperty("Type") val type: String,
    @field:JsonProperty("Subject") val subject: String,
    @field:JsonProperty("DataSet") val dataSet: DataSet,
    @field:JsonProperty("Rule") val rule: RuleGroupDefinition,
    @field:JsonProperty("CustomProperties") val customProperties: Map<String, String> = emptyMap(),
    @field:JsonProperty("Nodes") val nodes: List<Node?> = emptyList(),
) {
    companion object {
        fun fromDisplayRule(
            rule: DisplayRule,
            projectConfig: ProjectConfig,
            variableStructure: VariableStructure,
            findVar: (String) -> Variable
        ): JrdDefinition {
            val nodes = mutableListOf<Node?>(null)

            val normalizedVariablePaths = variableStructure.structure.map { (_, variablePathData) ->
                variablePathData.path.resolve(variableStructure, findVar)
                    ?.let { removeDataFromVariablePath(it) } ?: ""
            }.filter { it.isNotBlank() }

            val variableTree = buildVariableTree(normalizedVariablePaths)
            val arraysAndSubtrees = treeToNode(variableTree, null)
            nodes.addAll(arraysAndSubtrees)

            val findVarWithNodes = { id: String ->
                val variable = findVar(id)
                val variablePathData = variableStructure.structure[id]
                val resolvedPath = variablePathData?.path?.resolve(variableStructure, findVar)
                if (!resolvedPath.isNullOrBlank()) {
                    val nodePath = resolvedPath.split('.') + variable.nameOrId()

                    if (!nodes.any { it?.nodePath == nodePath }) {
                        val node = Node(
                            nodePath = nodePath, varType = variable.dataType.toInteractiveDataType()
                        )
                        nodes.add(node)
                    }
                }

                variable
            }

            val baseTemplate = IcmPath.from(rule.baseTemplate ?: projectConfig.baseTemplatePath)
                .toMapInteractive(projectConfig.interactiveTenant)

            val ruleDef = requireNotNull(rule.definition) { "Display rule '${rule.id}' cannot be deployed because it has missing definition" }
            val value = JrdDefinition(
                type = "Rule", subject = rule.subject ?: "", dataSet = DataSet(type = "Template", master = baseTemplate),
                rule = RuleGroupDefinition.fromDisplayRuleGroup(ruleDef.group, variableStructure, findVarWithNodes),
                customProperties = emptyMap(),
                nodes = nodes
            )

            return value
        }
    }
}

@Serializable
data class DataSet(
    @field:JsonProperty("Type") val type: String, @field:JsonProperty("Master") val master: String
)

@Serializable
sealed interface RuleDefinitionItem

@Serializable
data class RuleGroupDefinition(
    @field:JsonProperty("Type") val type: RuleDefinitionGroupType,
    @field:JsonProperty("Items") val items: List<RuleDefinitionItem>,
    @field:JsonProperty("Negation") val negation: Boolean = false,
) : RuleDefinitionItem {
    companion object {
        fun fromDisplayRuleGroup(
            group: Group, variableStructure: VariableStructure, findVar: (String) -> Variable
        ): RuleGroupDefinition {
            return RuleGroupDefinition(
                type = when (group.operator) {
                    GroupOp.And -> RuleDefinitionGroupType.And
                    GroupOp.Or -> RuleDefinitionGroupType.Or
                },
                negation = group.negation,
                items = group.items.map {
                    when (it) {
                        is Binary -> RuleComparisonDefinition.fromDisplayRuleComparison(it, variableStructure, findVar)
                        is Group -> fromDisplayRuleGroup(it, variableStructure, findVar)
                    }
                },
            )
        }
    }
}

@Serializable
data class RuleComparisonDefinition(
    @field:JsonProperty("Type") val type: ComparisonOperator,
    @field:JsonProperty("Items") val items: List<RuleOperand>,
    @field:JsonProperty("Negation") val negation: Boolean,
) : RuleDefinitionItem {
    companion object {
        fun fromDisplayRuleComparison(
            comparison: Binary, variableStructure: VariableStructure, findVar: (String) -> Variable
        ): RuleComparisonDefinition {
            val (leftSuccess, left) = RuleOperand.fromLiteralOrFunctionCall(comparison.left, variableStructure, findVar)
            val (rightSuccess, right) = RuleOperand.fromLiteralOrFunctionCall(comparison.right, variableStructure, findVar)

            var (negation, type) = when (comparison.operator) {
                BinOp.Equals -> false to ComparisonOperator.Equals
                BinOp.NotEquals -> true to ComparisonOperator.NotEquals
                BinOp.EqualsCaseInsensitive -> false to ComparisonOperator.EqualsCaseInsensitive
                BinOp.NotEqualsCaseInsensitive -> true to ComparisonOperator.NotEqualsCaseInsensitive
                BinOp.GreaterThan -> false to ComparisonOperator.GreaterThan
                BinOp.GreaterOrEqualThan -> false to ComparisonOperator.GreaterOrEqualThan
                BinOp.LessThan -> false to ComparisonOperator.LessThan
                BinOp.LessOrEqualThen -> false to ComparisonOperator.LessOrEqualThen
                BinOp.Contains -> false to ComparisonOperator.Contains
                BinOp.ContainsCaseInsensitive -> false to ComparisonOperator.ContainsCaseInsensitive
                BinOp.NotContains -> true to ComparisonOperator.Contains
                BinOp.NotContainsCaseInsensitive -> true to ComparisonOperator.ContainsCaseInsensitive
                BinOp.BeginsWith -> false to ComparisonOperator.BeginsWith
                BinOp.BeginsWithCaseInsensitive -> false to ComparisonOperator.BeginsWithCaseInsensitive
                BinOp.NotBeginsWith -> true to ComparisonOperator.BeginsWith
                BinOp.NotBeginsWithCaseInsensitive -> true to ComparisonOperator.BeginsWithCaseInsensitive
                BinOp.EndsWith -> false to ComparisonOperator.EndsWith
                BinOp.EndsWithCaseInsensitive -> false to ComparisonOperator.EndsWithCaseInsensitive
                BinOp.NotEndsWith -> true to ComparisonOperator.EndsWith
                BinOp.NotEndsWithCaseInsensitive -> true to ComparisonOperator.EndsWithCaseInsensitive
            }

            return if (leftSuccess && rightSuccess) {
                RuleComparisonDefinition(type = type, negation = negation, items = listOf(left, right))
            } else {
                val script = comparison.operator.toScript(
                    InspireDocumentObjectBuilder.ScriptResult.Success(left.getValue()),
                    InspireDocumentObjectBuilder.ScriptResult.Success(right.getValue())
                    )
                val items = listOf(
                    ValueOperand(value = Str("String('${script.replace("'", "")}')")),
                    ValueOperand(value = Str("String('unmapped')")),
                )
                RuleComparisonDefinition(type = ComparisonOperator.Equals, negation = false, items = items)
            }

        }
    }
}

@Serializable
enum class RuleDefinitionGroupType {
    @JsonProperty("and")
    And,

    @JsonProperty("or")
    Or
}

@Serializable
sealed interface RuleOperand {
    companion object {
        fun fromLiteralOrFunctionCall(
            item: LiteralOrFunctionCall, variableStructure: VariableStructure, findVar: (String) -> Variable
        ): Pair<Boolean, RuleOperand> {
            return when (item) {
                // A rule with a function should not reach here, it is a bug elsewhere in the code
                is Function -> error("Functions are not supported in external display rules")
                is Literal -> {
                    when (item.dataType) {
                        LiteralDataType.String -> true to ValueOperand(value = Str(item.value.replace("\"", "\\\"")))
                        LiteralDataType.Boolean -> true to ValueOperand(value = Bool(item.value.toBoolean()))
                        LiteralDataType.Number -> true to ValueOperand(value = Float(item.value.toDouble()))
                        LiteralDataType.Variable -> {
                            val variableModel = findVar(item.value)
                            when (val script = variableToScript(item.value, null, variableStructure, findVar)) {
                                is InspireDocumentObjectBuilder.ScriptResult.Success -> {
                                    true to VariableOperand(
                                        variableName = script.variableScript.replace("\"", "\\\""),
                                        variableType = variableModel.dataType.toInteractiveDataType()
                                    )
                                }

                                is InspireDocumentObjectBuilder.ScriptResult.Failure -> {
                                    val variablePathData = variableStructure.structure[item.value]
                                    val variableName = variablePathData?.name ?: variableModel.nameOrId()
                                    false to VariableOperand(
                                        variableName = "$${variableName.replace("\"", "\\\"")}",
                                        variableType = DataType.String.toInteractiveDataType()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getValue(): String {
        return when (this) {
            is ValueOperand -> when (value) {
                is Str -> value.value
                is Bool -> value.value.toString()
                is Float -> value.value.toString()
            }
            is VariableOperand -> variableName
        }
    }
}

@Serializable
@JsonSerialize(using = ValueSerializer::class)
sealed interface Value

@JvmInline
@Serializable
value class Bool(val value: Boolean) : Value

@JvmInline
@Serializable
value class Str(val value: String) : Value

@JvmInline
@Serializable
value class Float(val value: Double) : Value

class ValueSerializer : StdSerializer<Value>(Value::class.java) {
    override fun serialize(value: Value, gen: JsonGenerator, provider: SerializationContext) {
        when (value) {
            is Bool -> gen.writeBoolean(value.value)
            is Str -> gen.writeString(value.value)
            is Float -> gen.writeNumber(value.value)
        }
    }
}


@Serializable
data class ValueOperand(
    @field:JsonProperty("Type") val type: String = "value", @field:JsonProperty("Value") val value: Value
) : RuleOperand

@Serializable
data class VariableOperand(
    @field:JsonProperty("Type") val type: String = "variable",
    @field:JsonProperty("Value") val variableName: String,
    @field:JsonProperty("VariableType") val variableType: String
) : RuleOperand

enum class ComparisonOperator {
    @JsonProperty("equal")
    Equals,

    @JsonProperty("equalCaseInsensitive")
    EqualsCaseInsensitive,

    @JsonProperty("equal")
    NotEquals,

    @JsonProperty("equalCaseInsensitive")
    NotEqualsCaseInsensitive,

    @JsonProperty("more")
    GreaterThan,

    @JsonProperty("moreequal")
    GreaterOrEqualThan,

    @JsonProperty("less")
    LessThan,

    @JsonProperty("lessequal")
    LessOrEqualThen,

    @JsonProperty("contains")
    Contains,

    @JsonProperty("containsCaseInsensitive")
    ContainsCaseInsensitive,

    @JsonProperty("beginWith")
    BeginsWith,

    @JsonProperty("beginWithCaseInsensitive")
    BeginsWithCaseInsensitive,

    @JsonProperty("endWith")
    EndsWith,

    @JsonProperty("endWithCaseInsensitive")
    EndsWithCaseInsensitive,
}

@Serializable
data class Node(
    @field:JsonProperty("Cls") val cls: String = "Variable",
    @field:JsonProperty("NodePath") val nodePath: List<String>,
    @field:JsonProperty("Type") val type: String = "DataVariable",
    @field:JsonProperty("VarType") val varType: String
)

fun treeToNode(
    tree: Map<String, VariablePathPart>, parent: VariablePathPart?, path: List<String> = listOf()
): List<Node> {
    return tree.flatMap { (name, part) ->
        val currentPath = when (parent) {
            is ArrayVariable -> path + "Value" + name
            null -> listOf("Data") + name
            else -> path + name
        }
        val currentNode = Node(
            nodePath = currentPath, varType = when (part) {
                is ArrayVariable -> "Array"
                is SubtreeVariable -> "Subtree"
            }
        )

        listOf(currentNode) + treeToNode(part.children, part, currentPath)
    }
}
