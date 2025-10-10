package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Function
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.tools.model.aVariable
import com.quadient.migration.tools.model.aDisplayRule
import com.quadient.migration.tools.model.aVariableStructureModel
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test
import com.quadient.wfdxml.internal.module.layout.LayoutImpl

class InspireDocumentObjectBuilderTest {

    @Test
    fun `simple display rule with single expression`() {
        val rule = aDisplayRule(
            Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return (String('A')==String('B'));""")
    }

    @Test
    fun `uppercase function`() {
        val rule = aDisplayRule(
            left = Function.UpperCase((Literal("B", LiteralDataType.String))),
            operator = BinOp.Equals,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return ((String('B')).toUpperCase()==String('B'));""")
    }

    @Test
    fun `lowercase function`() {
        val rule = aDisplayRule(
            left = Function.LowerCase(Literal("B", LiteralDataType.String)),
            operator = BinOp.Equals,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return ((String('B')).toLowerCase()==String('B'));""")
    }

    @Test
    fun `case insensitive equals`() {
        val rule = aDisplayRule(
            left = Literal("A", LiteralDataType.String),
            operator = BinOp.EqualsCaseInsensitive,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return (String('A').equalCaseInsensitive(String('B')));""")
    }

    @Test
    fun `case insensitive not equals`() {
        val rule = aDisplayRule(
            left = Literal("A", LiteralDataType.String),
            operator = BinOp.NotEqualsCaseInsensitive,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return ((not String('A').equalCaseInsensitive(String('B'))));""")
    }

    private fun DisplayRuleModel.toScript(): String {
        return definition?.toScript(
            layout = LayoutImpl(),
            variableStructure = aVariableStructureModel("some struct"),
            findVar = { aVariable(it) }
        ) ?: error("No definition")
    }
}