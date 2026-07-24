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


    @Test
    fun `operators serialize correctly`() {
        val variableStructure = aVariableStructure("struct")
        val allBinOpsRule = DisplayRuleBuilder("allBinOpsRule")
            .internal(false)
            .group {
                // Equality
                comparison { value("a").equals().value("a") }
                comparison { value("a").equalsCaseInsensitive().value("a") }
                comparison { value("a").notEquals().value("a") }
                comparison { value("a").notEqualsCaseInsensitive().value("a") }
                // Numeric ordering
                comparison { value(100.0).greaterThan().value(50.0) }
                comparison { value(100.0).greaterOrEqualThan().value(100.0) }
                comparison { value(50.0).lessThan().value(100.0) }
                comparison { value(50.0).lessOrEqualThan().value(50.0) }
                // Contains
                comparison { value("a").contains().value("a") }
                comparison { value("a").containsCaseInsensitive().value("a") }
                comparison { value("a").notContains().value("a") }
                comparison { value("a").notContainsCaseInsensitive().value("a") }
                // BeginsWith
                comparison { value("a").beginsWith().value("a") }
                comparison { value("a").beginsWithCaseInsensitive().value("a") }
                comparison { value("a").notBeginsWith().value("a") }
                comparison { value("a").notBeginsWithCaseInsensitive().value("a") }
                // EndsWith
                comparison { value("a").endsWith().value("a") }
                comparison { value("a").endsWithCaseInsensitive().value("a") }
                comparison { value("a").notEndsWith().value("a") }
                comparison { value("a").notEndsWithCaseInsensitive().value("a") }
            }.build()

        val result = Jrd.fromDisplayRule(allBinOpsRule, aProjectConfig(
            baseTemplatePath = "icm://Interactive/StandardPackage/BaseTemplates/templ.wfd",
            interactiveTenant = "StandardPackage",
        ), variableStructure, { name -> aVariable(name) })


        result.replace("\r\n", "\n").shouldBeEqualTo($$"""
            {
              "InteractivePlusJsonDefinition" : {
                "Type" : "Rule",
                "Subject" : "",
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
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "equalCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "equal",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "equalCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "more",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : 100.0
                    }, {
                      "Type" : "value",
                      "Value" : 50.0
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "moreequal",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : 100.0
                    }, {
                      "Type" : "value",
                      "Value" : 100.0
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "less",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : 50.0
                    }, {
                      "Type" : "value",
                      "Value" : 100.0
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "lessequal",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : 50.0
                    }, {
                      "Type" : "value",
                      "Value" : 50.0
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "contains",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "containsCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "contains",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "containsCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "beginWith",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "beginWithCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "beginWith",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "beginWithCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "endWith",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "endWithCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : false
                  }, {
                    "Type" : "endWith",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  }, {
                    "Type" : "endWithCaseInsensitive",
                    "Items" : [ {
                      "Type" : "value",
                      "Value" : "a"
                    }, {
                      "Type" : "value",
                      "Value" : "a"
                    } ],
                    "Negation" : true
                  } ],
                  "Negation" : false
                },
                "CustomProperties" : { },
                "Nodes" : [ null ]
              }
            }""".trimIndent())
    }
}