package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DocxMergeFieldsTest {

    @Test
    void "parseIfField extracts merge field comparison and styled branches"() {
        // given
        def mergeField = field(text(" MERGEFIELD PaymentFrequency ", "variable"))
        def conditional = field(
                text(" IF ", "condition"),
                mergeField,
                text(' = 4 "', "condition"),
                text("in equal ", "normal"),
                text("quarterly payments", "emphasis"),
                text('" "" ', "condition")
        )

        // when
        def result = DocxMergeFields.parseIfField(conditional)

        // then
        assert result.variableId == "PaymentFrequency"
        assert result.operator == "="
        assert result.rightOperand == "4"
        assert result.trueContent*.text == ["in equal ", "quarterly payments"]
        assert result.trueContent*.styleId == ["normal", "emphasis"]
        assert result.falseContent.isEmpty()
    }

    @Test
    void "parseIfField extracts quoted operand and false branch"() {
        // given
        def conditional = field(
                text(" if "),
                field(text(" MERGEFIELD InsuranceIncluded ")),
                text(' = "Y" "Insurance included" "No insurance" ')
        )

        // when
        def result = DocxMergeFields.parseIfField(conditional)

        // then
        assert result.variableId == "InsuranceIncluded"
        assert result.rightOperand == '"Y"'
        assert result.trueContent*.text == ["Insurance included"]
        assert result.falseContent*.text == ["No insurance"]
    }

    @Test
    void "parseIfField accepts omitted false branch and trailing field switch"() {
        // given
        def conditional = field(
                text(" IF "),
                field(text(" MERGEFIELD InsuranceIncluded ")),
                text(' = "Y" "Insurance included" \\* MERGEFORMAT ')
        )

        // when
        def result = DocxMergeFields.parseIfField(conditional)

        // then
        assert result.trueContent*.text == ["Insurance included"]
        assert result.falseContent.isEmpty()
    }

    @Test
    void "parseIfField treats an unterminated quote as running to the end like Word"() {
        // given
        def conditional = field(
                text(" IF "),
                field(text(" MERGEFIELD PaymentFrequency ")),
                text(' = 4 "starts here')
        )

        // when
        def result = DocxMergeFields.parseIfField(conditional)

        // then
        assert result.trueContent*.text == ["starts here"]
        assert result.falseContent.isEmpty()
    }

    @Test
    void "parseIfField keeps nested fields as branch content and accepts a bare nested field branch"() {
        // given
        def nestedIf = field(text(" IF "), field(text(" MERGEFIELD Item01Discount ")), text(' = "NONE" "" "discount applies" '))
        def conditional = field(
                text(" if "),
                field(text(' MERGEFIELD StartDate \\@"YYYYMMDD"')),
                text(' > "20250401" '),
                nestedIf,
                text(" ")
        )

        // when
        def result = DocxMergeFields.parseIfField(conditional)

        // then
        assert result.variableId == "StartDate"
        assert result.operator == ">"
        assert result.rightOperand == '"20250401"'
        assert result.trueContent == [nestedIf]
        assert result.falseContent.isEmpty()
    }

    @Test
    void "parseIfField accepts a nested field directly after the IF keyword"() {
        // given
        def conditional = field(
                text(" "),
                text("if"),
                field(text(" MERGEFIELD DiscountApplies ")),
                text(' = "Y" "yes" "no" ')
        )

        // when
        def result = DocxMergeFields.parseIfField(conditional)

        // then
        assert result.variableId == "DiscountApplies"
        assert result.trueContent*.text == ["yes"]
        assert result.falseContent*.text == ["no"]
    }

    @ParameterizedTest
    @CsvSource(value = [
            "=|Equals",
            "<>|NotEquals",
            ">|GreaterThan",
            "<|LessThan",
            ">=|GreaterOrEqualThan",
            "<=|LessOrEqualThen",
    ], delimiterString = '|')
    void "parseConditions directly maps every supported Word operator"(String wordOperator, String expectedOperator) {
        // when
        def definition = DocxMergeFields.parseConditions([new IfCondition(
                variableId: "PaymentFrequency",
                operator: wordOperator,
                rightOperand: "4"
        )])

        // then
        Binary comparison = definition.group.items[0] as Binary
        def left = comparison.left as Literal
        assert left.value == "PaymentFrequency"
        assert left.dataType == LiteralDataType.Variable
        assert comparison.operator == BinOp.valueOf(expectedOperator)
        def right = comparison.right as Literal
        assert right.value == "4"
        assert right.dataType == LiteralDataType.Number
        assert !definition.group.negation
    }

    @Test
    void "parseConditions preserves quoted strings and inverts the operator of an else condition"() {
        // when
        def definition = DocxMergeFields.parseConditions([new IfCondition(
                variableId: "InsuranceIncluded",
                operator: "=",
                rightOperand: '"Y"',
                negated: true
        )])

        // then
        Binary comparison = definition.group.items[0] as Binary
        assert comparison.operator == BinOp.NotEquals
        def right = comparison.right as Literal
        assert right.value == "Y"
        assert right.dataType == LiteralDataType.String
        assert !definition.group.negation
    }

    @Test
    void "parseConditions ANDs the conditions of a nested IF path into one flat group"() {
        // when
        def definition = DocxMergeFields.parseConditions([
                new IfCondition(variableId: "StartDate", operator: ">", rightOperand: '"20250401"'),
                new IfCondition(variableId: "Item01Discount", operator: "=", rightOperand: '"NONE"', negated: true),
        ])

        // then
        assert definition.group.operator == GroupOp.And
        assert definition.group.items.size() == 2
        assert (definition.group.items[1] as Binary).operator == BinOp.NotEquals
    }

    private static WordField field(Object... parts) {
        return new WordField(instructionParts: parts as List)
    }

    private static FieldTextPart text(String value, String styleId = "style") {
        return new FieldTextPart(text: value, styleId: styleId)
    }

    @ParameterizedTest
    @ValueSource(strings = [" MERGEFIELD PolicyHolderTitle ", "MERGEFIELD Name", "mergefield Name", " MERGEFIELD \"Full Name\" \\* MERGEFORMAT "])
    void "isMergeField recognizes MERGEFIELD instructions regardless of case or spacing"(String instruction) {
        assert DocxMergeFields.isMergeField(instruction)
    }

    @ParameterizedTest
    @ValueSource(strings = [" IF ", " PAGEREF _Toc123 \\h ", " TOC \\o \"1-3\" \\h \\z \\u ", "", " ", "MERGE"])
    void "isMergeField rejects non-MERGEFIELD or blank instructions"(String instruction) {
        assert !DocxMergeFields.isMergeField(instruction)
    }

    @Test
    void "isMergeField rejects null instruction"() {
        assert !DocxMergeFields.isMergeField(null)
    }

    @ParameterizedTest
    @CsvSource(value = [
            " MERGEFIELD PolicyHolderTitle |PolicyHolderTitle",
            "MERGEFIELD Name|Name",
            "MERGEFIELD Name \\* MERGEFORMAT|Name",
            "MERGEFIELD \"Full Name\" \\* MERGEFORMAT|Full Name",
            "MERGEFIELD \"Full Name\"|Full Name",
            "MERGEFIELD TotalAmount\\#,0.00|TotalAmount",
            "MERGEFIELD TotalAmount \\# ,0.00|TotalAmount",
    ], delimiterString = '|')
    void "extractMergeFieldName extracts the field name, unquoting and stripping switches"(String instruction, String expectedName) {
        assert DocxMergeFields.extractMergeFieldName(instruction) == expectedName
    }

    @Test
    void "extractMergeFieldName returns null for blank or missing name"() {
        assert DocxMergeFields.extractMergeFieldName(null) == null
        assert DocxMergeFields.extractMergeFieldName("") == null
        assert DocxMergeFields.extractMergeFieldName("MERGEFIELD") == null
        assert DocxMergeFields.extractMergeFieldName("MERGEFIELD ") == null
    }
}
