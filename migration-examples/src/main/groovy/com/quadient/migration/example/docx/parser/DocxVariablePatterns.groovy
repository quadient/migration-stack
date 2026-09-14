package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.shared.DataType

import java.util.regex.Pattern

/**
 * Converts project-configured, text-based variable markers (for example ${Name} or <<Name>>) to variable references.
 * Each regular expression must contain one capturing group: group 1 becomes the variable name.
 */
class DocxVariablePatterns {
    static final String CONTEXT_KEY = "docxVariablePatterns"

    static void addText(Migration migration, List<ParagraphBuilder.TextBuilder> textBuilders, String text, String styleId,
                        String fileName) {
        List<Pattern> patterns = configuredPatterns(migration)
        if (!text || patterns.isEmpty()) {
            addLiteral(textBuilders, text, styleId)
            return
        }

        List<VariableMatch> matches = patterns.collectMany { pattern ->
            def matcher = pattern.matcher(text)
            def result = []
            while (matcher.find()) {
                String variableId = matcher.groupCount() >= 1 ? matcher.group(1)?.trim() : null
                if (variableId) {
                    result.add(new VariableMatch(start: matcher.start(), end: matcher.end(), variableId: variableId))
                }
            }
            result
        }.sort { left, right -> left.start <=> right.start ?: right.end <=> left.end }

        int offset = 0
        matches.each { match ->
            // First configured pattern wins for overlapping matches, avoiding duplicate variable references.
            if (match.start < offset) {
                return
            }
            addLiteral(textBuilders, text.substring(offset, match.start), styleId)
            ensureVariable(migration, match.variableId, fileName)
            textBuilders.add(new ParagraphBuilder.TextBuilder().variableRef(match.variableId).styleRef(styleId))
            offset = match.end
        }
        addLiteral(textBuilders, text.substring(offset), styleId)
    }

    private static List<Pattern> configuredPatterns(Migration migration) {
        def configured = migration.projectConfig.context?.get(CONTEXT_KEY)
        Collection values = configured instanceof Collection ? configured : configured == null ? [] : [configured]
        return values.collect { value ->
            try {
                Pattern pattern = value instanceof Pattern ? value : Pattern.compile(value.toString())
                if (pattern.matcher("").groupCount() < 1) {
                    println "  Warning: DOCX variable pattern '${value}' has no capture group; it is ignored."
                    return null
                }
                pattern
            } catch (Exception e) {
                println "  Warning: Invalid DOCX variable pattern '${value}': ${e.message}"
                null
            }
        }.findAll()
    }

    private static void addLiteral(List<ParagraphBuilder.TextBuilder> textBuilders, String text, String styleId) {
        if (text) {
            textBuilders.add(new ParagraphBuilder.TextBuilder().string(text).styleRef(styleId))
        }
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

    private static class VariableMatch {
        int start
        int end
        String variableId
    }
}
