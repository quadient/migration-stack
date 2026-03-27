package com.quadient.migration.shared

import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.aVariable
import com.quadient.migration.tools.model.aVariableStructure
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class JrdDefinitionTest {
    val projectConfig = aProjectConfig(
        baseTemplatePath = "icm://Interactive/StandardPackage/BaseTemplates/templ.wfd",
        interactiveTenant = "StandardPackage",
    )

    @Test
    fun `serializes correctly`() {
        val groupItems = listOf(
            Binary(
                left = Literal(value = "field1", dataType = LiteralDataType.String ),
                operator = BinOp.EqualsCaseInsensitive,
                right = Literal(value = "variable1", dataType = LiteralDataType.Variable)
            ),
        )
        val variableStructure = aVariableStructure("struct", structure = mapOf(
            "variable1" to VariablePathData(path = "Data.Clients.Value", name = null),
            "birth" to VariablePathData(path = "Data.Clients.Value.PersonalData", name = null),
            "city" to VariablePathData(path = "Data.Clients.Value.PersonalData", name = null),
        ))

        val rule = DisplayRuleBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .subject("Test Subject")
            .originLocations(listOf("test1", "test2"))
            .definition(DisplayRuleDefinition(group = Group(items = groupItems, operator = GroupOp.Or, false)))
            .build()

        val result = JrdDefinition.fromDisplayRule(rule, projectConfig, variableStructure, { name -> aVariable(name) })
        val json = Jrd.fromDisplayRule(rule, projectConfig, variableStructure, { name -> aVariable(name) })

        result.subject.shouldBeEqualTo("Test Subject")
        result.dataSet.type.shouldBeEqualTo("Template")
        result.dataSet.master.shouldBeEqualTo("map://interactive/BaseTemplates/templ.wfd")
        result.type.shouldBeEqualTo("Rule")
        result.customProperties.shouldBeEqualTo(emptyMap())
        result.nodes.shouldBeEqualTo(listOf(
            null,
            Node(cls = "Variable", nodePath = listOf("Data", "Clients"), type = "DataVariable", varType = "Array"),
            Node(cls = "Variable", nodePath = listOf("Data", "Clients", "Value", "PersonalData"), type = "DataVariable", varType = "Subtree"),
            Node(cls = "Variable", nodePath = listOf("Data", "Clients", "Value", "variablevariable1"), type = "DataVariable", varType = "String"),
        ))
        result.rule.shouldBeEqualTo(RuleGroupDefinition(
            type = RuleDefinitionGroupType.Or,
            items = listOf(
                RuleComparisonDefinition(
                    type = ComparisonOperator.EqualsCaseInsensitive,
                    negation = false,
                    items = listOf(
                        ValueOperand(type = "value", value = Str("field1")),
                        VariableOperand(type = "variable", variableName = "DATA.Clients.Current.variablevariable1", variableType = "String")
                    )
                )
            ),
            negation = false
        ))
        json.replace("\r\n", "\n").shouldBeEqualTo("""
        {
          "InteractivePlusJsonDefinition" : {
            "Type" : "Rule",
            "Subject" : "Test Subject",
            "DataSet" : {
              "Type" : "Template",
              "Master" : "map://interactive/BaseTemplates/templ.wfd"
            },
            "Rule" : {
              "Type" : "or",
              "Items" : [ {
                "Type" : "equalCaseInsensitive",
                "Items" : [ {
                  "Type" : "value",
                  "Value" : "field1"
                }, {
                  "value" : "DATA.Clients.Current.variablevariable1",
                  "Type" : "variable",
                  "Value" : "DATA.Clients.Current.variablevariable1",
                  "VariableType" : "String"
                } ],
                "Negation" : false
              } ],
              "Negation" : false
            },
            "CustomProperties" : { },
            "Nodes" : [ null, {
              "Cls" : "Variable",
              "NodePath" : [ "Data", "Clients" ],
              "Type" : "DataVariable",
              "VarType" : "Array"
            }, {
              "Cls" : "Variable",
              "NodePath" : [ "Data", "Clients", "Value", "PersonalData" ],
              "Type" : "DataVariable",
              "VarType" : "Subtree"
            }, {
              "Cls" : "Variable",
              "NodePath" : [ "Data", "Clients", "Value", "variablevariable1" ],
              "Type" : "DataVariable",
              "VarType" : "String"
            } ]
          }
        }""".trimIndent())
    }

    @Test
    fun `unmapped variables serialize with placeholder`() {
        val groupItems = listOf(
            Binary(
                left = Literal(value = "field1", dataType = LiteralDataType.String ),
                operator = BinOp.EqualsCaseInsensitive,
                right = Literal(value = "variable1", dataType = LiteralDataType.Variable)
            ),
        )
        val variableStructure = aVariableStructure("struct")

        val rule = DisplayRuleBuilder("id")
            .customFields(mutableMapOf("f1" to "val1"))
            .subject("Test Subject")
            .originLocations(listOf("test1", "test2"))
            .definition(DisplayRuleDefinition(group = Group(items = groupItems, operator = GroupOp.And, false)))
            .build()

        val result = Jrd.fromDisplayRule(rule, aProjectConfig(
            baseTemplatePath = "icm://Interactive/StandardPackage/BaseTemplates/templ.wfd",
            interactiveTenant = "StandardPackage",
        ), variableStructure, { name -> aVariable(name) })

         result.replace("\r\n", "\n").shouldBeEqualTo($$"""
            {
              "InteractivePlusJsonDefinition" : {
                "Type" : "Rule",
                "Subject" : "Test Subject",
                "DataSet" : {
                  "Type" : "Template",
                  "Master" : "map://interactive/BaseTemplates/templ.wfd"
                },
                "Rule" : {
                  "Type" : "and",
                  "Items" : [ {
                    "Type" : "equal",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "String('field1.equalCaseInsensitive($variablevariable1)')"
                    }, {
                      "Type" : "value",
                      "Value" : "String('unmapped')"
                    } ],
                    "Negation" : false
                  } ],
                  "Negation" : false
                },
                "CustomProperties" : { },
                "Nodes" : [ null ]
              }
            }""".trimIndent())
    }
}