package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import groovy.transform.Field
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.xmlbeans.XmlObject
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText

import static com.quadient.migration.example.docx.util.DocxUtils.sha256Hex

interface FieldPart {}

class FieldTextPart implements FieldPart {
    String text
    String styleId
}

// A body-level table that Word placed inside a field instruction (a whole table wrapped in an IF field).
class FieldTablePart implements FieldPart {
    XWPFTable table
}

class WordField implements FieldPart {
    List<FieldPart> instructionParts = []
    String styleId
    boolean resultStarted = false

    void addPart(FieldPart part) {
        // Only the instruction is interpreted; the cached field result (after "separate") is ignored.
        if (!resultStarted) {
            instructionParts.add(part)
        }
    }

    String instructionText() {
        return instructionParts.findAll { it instanceof FieldTextPart }
                .collect { (it as FieldTextPart).text }
                .join("")
    }

    boolean containsTable() {
        return instructionParts.any { it instanceof FieldTablePart || (it instanceof WordField && it.containsTable()) }
    }
}

class FieldParseState {
    List<WordField> stack = []
    List<ParsedIfField> pendingIfFields = []
    boolean pendingFirstMatchEligible = true
    // Receives (XWPFTable, displayRuleId) for tables wrapped in a resolved IF field; null when tables cannot occur.
    Closure conditionalTableHandler

    int getDepth() {
        return stack.size()
    }

    boolean isInsideFieldResult() {
        return stack.any { it.resultStarted }
    }
}

class IfCondition {
    String variableId
    String operator
    String rightOperand
    boolean negated

    String canonical() {
        return "${variableId} ${effectiveOperator()} ${rightOperand}"
    }

    // A negated (else-branch) condition is expressed by inverting the operator so the rule stays a flat AND group.
    String effectiveOperator() {
        if (!negated) {
            return operator
        }
        return switch (operator) {
            case "=" -> "<>"
            case "<>" -> "="
            case ">" -> "<="
            case "<=" -> ">"
            case "<" -> ">="
            case ">=" -> "<"
            default -> throw new IllegalArgumentException("Unsupported Word IF operator: ${operator}")
        }
    }

    Literal operandLiteral() {
        if (rightOperand.startsWith('"')) {
            return new Literal(rightOperand.substring(1, rightOperand.length() - 1), LiteralDataType.String)
        }
        return new Literal(rightOperand, LiteralDataType.Number)
    }
}

class ParsedIfField {
    String variableId
    String operator
    String rightOperand
    // Branch content is text, nested fields (IF / MERGEFIELD) or tables, in source order.
    List<FieldPart> trueContent
    List<FieldPart> falseContent

    String condition() {
        return "${variableId} ${operator} ${rightOperand}"
    }

    IfCondition toCondition(boolean negated) {
        return new IfCondition(variableId: variableId, operator: operator, rightOperand: rightOperand, negated: negated)
    }

    Literal operandLiteral() {
        return toCondition(false).operandLiteral()
    }
}

@Field
static final char PLACEHOLDER = (char) 0xE000

static List<XmlObject> fieldChildren(XWPFRun run) {
    return run.CTR.selectPath("./*").findAll { it.domNode.localName in ["fldChar", "instrText"] }
}

static boolean isFieldBegin(XmlObject child) {
    return child.domNode.localName == "fldChar" && (child as CTFldChar).fldCharType.toString() == "begin"
}

static boolean isInstructionText(XmlObject child) {
    return child.domNode.localName == "instrText"
}

static String instructionText(List<XmlObject> fieldChildren) {
    return fieldChildren.findAll { isInstructionText(it) }.collect { (it as CTText).stringValue }.join("")
}

static void handleFieldChildren(Migration migration, List<XmlObject> fieldChildren, String textStyleId, FieldParseState state,
                                List<ParagraphBuilder.TextBuilder> textBuilders, String fileName) {
    fieldChildren.each { XmlObject child ->
        if (child.domNode.localName == "fldChar") {
            String type = (child as CTFldChar).fldCharType.toString()
            handleFieldCharacter(migration, type, textStyleId, state, textBuilders, fileName)
        } else if (state.depth > 0) {
            state.stack.last().addPart(new FieldTextPart(text: (child as CTText).stringValue, styleId: textStyleId))
        }
    }
}

// Called for a body-level table encountered while a field is open; returns false when the table is a cached field result
// (content between "separate" and "end") that must not be emitted at all.
static boolean handleTableInsideField(FieldParseState state, XWPFTable table) {
    if (state.insideFieldResult) {
        return false
    }
    state.stack.last().addPart(new FieldTablePart(table: table))
    return true
}

private static void handleFieldCharacter(Migration migration, String type, String textStyleId, FieldParseState state,
                                         List<ParagraphBuilder.TextBuilder> textBuilders, String fileName) {
    switch (type) {
        case "begin":
            WordField field = new WordField(styleId: textStyleId)
            if (state.depth > 0) {
                state.stack.last().addPart(field)
            }
            state.stack.add(field)
            break
        case "separate":
            if (state.depth > 0) {
                state.stack.last().resultStarted = true
            }
            break
        case "end":
            if (state.depth == 0) {
                return
            }
            WordField completed = state.stack.removeLast()
            if (state.depth == 0) {
                resolveField(migration, state, textBuilders, fileName, completed)
            }
            break
    }
}

private static void resolveField(Migration migration, FieldParseState state, List<ParagraphBuilder.TextBuilder> textBuilders, String fileName,
                                 WordField field) {
    String instruction = field.instructionText()
    if (isMergeField(instruction)) {
        // A MERGEFIELD (or an unsupported field below) breaks a chain of IFs.
        flushPendingIfFields(migration, state, textBuilders, fileName)
        addMergeField(migration, textBuilders, fileName, instruction, field.styleId)
        return
    }

    ParsedIfField parsed = parseIfField(field)
    if (parsed == null) {
        flushPendingIfFields(migration, state, textBuilders, fileName)
        if (field.containsTable()) {
            println "  Warning: Unsupported field wrapping a table, its tables are dropped: '${instruction.trim()}'"
        }
        return
    }

    ensureVariable(migration, parsed.variableId, fileName)
    if (!state.pendingIfFields.isEmpty() && state.pendingIfFields.first().variableId != parsed.variableId) {
        flushPendingIfFields(migration, state, textBuilders, fileName)
    }
    state.pendingFirstMatchEligible = state.pendingIfFields.isEmpty()
            ? isFirstMatchCandidate(parsed)
            : state.pendingFirstMatchEligible && canJoinFirstMatch(state.pendingIfFields, parsed)
    state.pendingIfFields.add(parsed)
}

static void flushPendingIfFields(Migration migration, FieldParseState state, List<ParagraphBuilder.TextBuilder> textBuilders, String fileName) {
    if (state.pendingIfFields.isEmpty()) {
        return
    }

    if (state.pendingIfFields.size() > 1 && state.pendingFirstMatchEligible) {
        addFirstMatch(migration, textBuilders, fileName, state.pendingIfFields)
    } else {
        state.pendingIfFields.each { addIndependentConditional(migration, state, textBuilders, fileName, it) }
    }
    state.pendingIfFields.clear()
    state.pendingFirstMatchEligible = true
}

private static void addIndependentConditional(Migration migration, FieldParseState state, List<ParagraphBuilder.TextBuilder> textBuilders,
                                              String fileName, ParsedIfField parsed) {
    boolean blockLevel = containsTable(parsed.trueContent) || containsTable(parsed.falseContent)
    emitBranch(migration, state, textBuilders, fileName, parsed.trueContent, [parsed.toCondition(false)], blockLevel)
    emitBranch(migration, state, textBuilders, fileName, parsed.falseContent, [parsed.toCondition(true)], blockLevel)
}

// Emits one IF branch; every nested IF extends the condition path, so each leaf gets a rule that ANDs all conditions
// leading to it. Whitespace-only text is dropped for fields wrapping tables, where it is just field-code formatting.
private static void emitBranch(Migration migration, FieldParseState state, List<ParagraphBuilder.TextBuilder> textBuilders, String fileName,
                               List<FieldPart> parts, List<IfCondition> path, boolean blockLevel) {
    parts.each { FieldPart part ->
        if (part instanceof FieldTextPart) {
            if (part.text && !(blockLevel && part.text.isBlank())) {
                textBuilders.add(new ParagraphBuilder.TextBuilder()
                        .string(part.text)
                        .styleRef(part.styleId)
                        .displayRuleRef(upsertDisplayRule(migration, path, fileName)))
            }
        } else if (part instanceof FieldTablePart) {
            if (state.conditionalTableHandler != null) {
                state.conditionalTableHandler.call(part.table, upsertDisplayRule(migration, path, fileName))
            } else {
                println "  Warning: Table inside IF field is not supported in this context and is dropped."
            }
        } else if (part instanceof WordField) {
            emitNestedField(migration, state, textBuilders, fileName, part, path, blockLevel)
        }
    }
}

private static void emitNestedField(Migration migration, FieldParseState state, List<ParagraphBuilder.TextBuilder> textBuilders, String fileName,
                                    WordField field, List<IfCondition> path, boolean blockLevel) {
    String instruction = field.instructionText()
    if (isMergeField(instruction)) {
        String variableId = extractMergeFieldName(instruction)
        if (variableId) {
            ensureVariable(migration, variableId, fileName)
            textBuilders.add(new ParagraphBuilder.TextBuilder()
                    .variableRef(variableId)
                    .styleRef(field.styleId)
                    .displayRuleRef(upsertDisplayRule(migration, path, fileName)))
        }
        return
    }
    ParsedIfField nested = parseIfField(field)
    if (nested == null) {
        println "  Warning: Unsupported nested field inside IF branch is dropped: '${instruction.trim()}'"
        return
    }
    ensureVariable(migration, nested.variableId, fileName)
    emitBranch(migration, state, textBuilders, fileName, nested.trueContent, path + [nested.toCondition(false)], blockLevel)
    emitBranch(migration, state, textBuilders, fileName, nested.falseContent, path + [nested.toCondition(true)], blockLevel)
}

private static boolean containsTable(List<FieldPart> parts) {
    return parts.any { it instanceof FieldTablePart || (it instanceof WordField && it.containsTable()) }
}

private static void addFirstMatch(Migration migration, List<ParagraphBuilder.TextBuilder> textBuilders, String fileName,
                                  List<ParsedIfField> fields) {
    List<FirstMatch.Case> cases = fields.collect { field ->
        String ruleId = upsertDisplayRule(migration, [field.toCondition(false)], fileName)
        new FirstMatch.Case(
                new DisplayRuleRef(ruleId),
                [new StringValue(field.trueContent*.text.join(""))],
                field.condition()
        )
    }
    textBuilders.add(new ParagraphBuilder.TextBuilder()
            .styleRef((fields.first().trueContent.first() as FieldTextPart).styleId)
            .firstMatch(new FirstMatch(cases, [])))
}

private static boolean canJoinFirstMatch(List<ParsedIfField> fields, ParsedIfField candidate) {
    return isFirstMatchCandidate(candidate)
            && fields.every { isFirstMatchCandidate(it) }
            && fields.first().variableId == candidate.variableId
            && (fields.first().trueContent.first() as FieldTextPart).styleId == (candidate.trueContent.first() as FieldTextPart).styleId
            && fields.every { it.operandLiteral() != candidate.operandLiteral() }
}

private static boolean isFirstMatchCandidate(ParsedIfField field) {
    return field.operator == "="
            && field.falseContent.isEmpty()
            && field.trueContent.size() == 1
            && field.trueContent.first() instanceof FieldTextPart
            && (field.trueContent.first() as FieldTextPart).text
}

private static void addMergeField(Migration migration, List<ParagraphBuilder.TextBuilder> textBuilders, String fileName,
                                  String fieldInstruction, String textStyleId) {
    String variableId = extractMergeFieldName(fieldInstruction)
    if (!variableId) {
        return
    }
    ensureVariable(migration, variableId, fileName)
    textBuilders.add(new ParagraphBuilder.TextBuilder()
            .variableRef(variableId)
            .styleRef(textStyleId))
}

private static void ensureVariable(Migration migration, String variableId, String fileName) {
    if (migration.variableRepository.find(variableId) == null) {
        migration.variableRepository.upsert(new VariableBuilder(variableId)
                .name(variableId)
                .originLocations([fileName])
                .dataType(DataType.String)
                .build())
    }
}

static String upsertDisplayRule(Migration migration, List<IfCondition> conditions, String fileName) {
    String canonical = conditions*.canonical().join(" AND ")
    String ruleId = "docx_if_${sha256Hex(canonical).substring(0, 16)}"
    if (migration.displayRuleRepository.find(ruleId) == null) {
        migration.displayRuleRepository.upsert(new DisplayRuleBuilder(ruleId)
                .name(canonical)
                .originLocations([fileName])
                .addCustomField("originContent", canonical)
                .definition(parseConditions(conditions))
                .build())
    }
    return ruleId
}

static DisplayRuleDefinition parseConditions(List<IfCondition> conditions) {
    List<Binary> comparisons = conditions.collect { IfCondition condition ->
        BinOp operator = switch (condition.effectiveOperator()) {
            case "=" -> BinOp.Equals
            case "<>" -> BinOp.NotEquals
            case ">" -> BinOp.GreaterThan
            case "<" -> BinOp.LessThan
            case ">=" -> BinOp.GreaterOrEqualThan
            case "<=" -> BinOp.LessOrEqualThen
            default -> throw new IllegalArgumentException("Unsupported Word IF operator: ${condition.operator}")
        }
        new Binary(new Literal(condition.variableId, LiteralDataType.Variable), operator, condition.operandLiteral())
    }
    return new DisplayRuleDefinition(new Group(comparisons, GroupOp.And, false))
}

class InstructionSpan {
    int start
    int end
    FieldPart part
}

class Token {
    int contentStart
    int contentEnd
    int end
}

static ParsedIfField parseIfField(WordField field) {
    List<InstructionSpan> spans = []
    StringBuilder flattened = new StringBuilder()

    // Nested fields and tables are replaced by private-use placeholder chars so the instruction can be scanned as one string.
    field.instructionParts.each { FieldPart part ->
        int start = flattened.length()
        if (part instanceof FieldTextPart) {
            flattened.append(part.text)
        } else {
            flattened.append(PLACEHOLDER)
        }
        spans.add(new InstructionSpan(start: start, end: flattened.length(), part: part))
    }

    String input = flattened.toString()
    int index = skipWhitespace(input, 0)
    if (!startsWithWordIgnoreCase(input, index, "IF")) {
        return null
    }
    index = skipWhitespace(input, index + 2)
    if (index >= input.length() || input.charAt(index) != PLACEHOLDER) {
        return null
    }

    FieldPart left = spans.find { it.start == index && !(it.part instanceof FieldTextPart) }?.part
    if (!(left instanceof WordField)) {
        return null
    }
    String variableId = extractMergeFieldName((left as WordField).instructionText())
    if (!variableId) {
        return null
    }

    index = skipWhitespace(input, index + 1)
    String operator = [">=", "<=", "<>", "=", ">", "<"].find { input.startsWith(it, index) }
    if (!operator) {
        return null
    }
    index = skipWhitespace(input, index + operator.length())

    Token right = readOperand(input, index)
    if (right == null) {
        return null
    }
    String rightOperand = input.substring(right.contentStart, right.end)
    if (!isSupportedOperand(rightOperand)) {
        return null
    }

    Token trueBranch = readBranch(input, skipWhitespace(input, right.end))
    if (trueBranch == null) {
        return null
    }
    // Word ignores anything after the false branch (switches, stray quotes), so no trailing validation is done.
    Token falseBranch = readBranch(input, skipWhitespace(input, trueBranch.end))

    return new ParsedIfField(
            variableId: variableId,
            operator: operator,
            rightOperand: rightOperand,
            trueContent: extractRange(spans, trueBranch),
            falseContent: falseBranch == null ? [] : extractRange(spans, falseBranch)
    )
}

private static boolean isSupportedOperand(String operand) {
    return (operand.length() >= 2 && operand.startsWith('"') && operand.endsWith('"'))
            || operand ==~ /[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?/
}

// The returned token spans the whole operand including quotes, so contentStart is the operand start.
private static Token readOperand(String input, int start) {
    if (start >= input.length()) {
        return null
    }
    if (input.charAt(start) == '"') {
        Token quoted = readQuoted(input, start)
        return quoted == null ? null : new Token(contentStart: start, contentEnd: quoted.end, end: quoted.end)
    }
    return readBareToken(input, start)
}

// A branch is either a quoted string or a single bare token (typically just a nested field placeholder).
private static Token readBranch(String input, int start) {
    if (start >= input.length()) {
        return null
    }
    if (input.charAt(start) == '"') {
        return readQuoted(input, start)
    }
    if (input.charAt(start) == '\\') {
        return null
    }
    return readBareToken(input, start)
}

private static Token readBareToken(String input, int start) {
    int end = start
    while (end < input.length() && !Character.isWhitespace(input.charAt(end))) {
        end++
    }
    return end == start ? null : new Token(contentStart: start, contentEnd: end, end: end)
}

// Like Word, an unterminated quote runs to the end of the instruction.
private static Token readQuoted(String input, int start) {
    if (start >= input.length() || input.charAt(start) != '"') {
        return null
    }
    int index = start + 1
    while (index < input.length()) {
        if (input.charAt(index) == '"' && (index == start + 1 || input.charAt(index - 1) != '\\')) {
            return new Token(contentStart: start + 1, contentEnd: index, end: index + 1)
        }
        index++
    }
    return new Token(contentStart: start + 1, contentEnd: input.length(), end: input.length())
}

private static List<FieldPart> extractRange(List<InstructionSpan> spans, Token range) {
    List<FieldPart> result = []
    spans.each { InstructionSpan span ->
        int overlapStart = Math.max(range.contentStart, span.start)
        int overlapEnd = Math.min(range.contentEnd, span.end)
        if (overlapStart >= overlapEnd) {
            return
        }
        if (!(span.part instanceof FieldTextPart)) {
            result.add(span.part)
            return
        }
        FieldTextPart textPart = span.part as FieldTextPart
        String text = textPart.text.substring(overlapStart - span.start, overlapEnd - span.start)
        FieldTextPart previous = result && result.last() instanceof FieldTextPart ? result.last() as FieldTextPart : null
        // Whitespace carries no visible formatting, so it is glued to the neighbouring text instead of becoming a
        // separate (ruled) text of its own.
        if (previous != null && (previous.styleId == textPart.styleId || text.isBlank())) {
            previous.text += text
        } else if (previous != null && previous.text.isBlank()) {
            previous.text += text
            previous.styleId = textPart.styleId
        } else {
            result.add(new FieldTextPart(text: text, styleId: textPart.styleId))
        }
    }
    return result
}

private static int skipWhitespace(String input, int index) {
    while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
        index++
    }
    return index
}

// A nested field directly after the keyword (e.g. "if«MERGEFIELD X»") also terminates the word, as in Word.
private static boolean startsWithWordIgnoreCase(String input, int index, String word) {
    if (index + word.length() > input.length()
            || !input.regionMatches(true, index, word, 0, word.length())) {
        return false
    }
    int end = index + word.length()
    return end == input.length() || Character.isWhitespace(input.charAt(end)) || input.charAt(end) == PLACEHOLDER
}

static boolean isMergeField(String fieldInstruction) {
    return fieldInstruction?.trim()?.toUpperCase(Locale.ROOT)?.startsWith("MERGEFIELD")
}

static String extractMergeFieldName(String fieldInstruction) {
    String instr = fieldInstruction?.trim()
    if (!instr) {
        return null
    }
    String remainder = instr.replaceFirst(/(?i)^MERGEFIELD\b\s*/, "").trim()
    if (!remainder) {
        return null
    }
    def quoted = (remainder =~ /^"([^"]+)"/)
    if (quoted.find()) {
        return quoted.group(1).trim()
    }
    def switchMatcher = (remainder =~ /\\\S/)
    String name = switchMatcher.find() ? remainder.substring(0, switchMatcher.start()) : remainder
    name = name.trim()
    return name ?: null
}
