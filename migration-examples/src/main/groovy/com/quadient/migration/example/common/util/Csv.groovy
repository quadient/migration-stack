package com.quadient.migration.example.common.util

import com.quadient.migration.api.dto.migrationmodel.Tab
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size

static String serialize(Object obj) {
    return serialize(obj, Size.Unit.Millimeters)
}

static List<String> split(String line) {
    if (line == null) return []
    if (line.empty) return [""]

    List<String> columns = []
    int i = 0
    def inQuotes = false
    def inQuotedField = false
    def currentField = new StringBuilder()

    while (i < line.length()) {
        def c = line.charAt(i) as Character
        def next = i < line.length() - 1 ? line.charAt(i + 1) : null

        if (inQuotes) {
            // Escaped quote by another quote as specified by RFC4180
            if (c == '"' && next == '"') {
                currentField << '"'
                i++ // Increment to skip the next quote
            } else if (c == '"') {
                // End of quoted field
                inQuotes = false
            } else {
                currentField << c
            }
        } else {
            // Field boundary
            if (c == ",") {
                inQuotedField = false
                columns.add(currentField.toString())
                currentField.setLength(0)
            } else if (currentField.length() == 0 && c == '"' && !inQuotedField) {
                // Start of a quoted field
                inQuotes = true
                inQuotedField = true
            } else {
                currentField << c
            }
        }

        i++
    }

    columns.add(currentField.toString())

    return columns
}

static Map<String, String> getCells(String line, List<String> columnNames) {
    if (!line) return new HashMap<String, String>()

    def columns = split(line)
    def values = new HashMap<String, String>()
    for (i in 0..<columnNames.size()) {
        values.put(columnNames[i], columns[i])
    }

    return values
}

static List<String> parseColumnNames(String line) {
    if (!line || line.empty) return []

    // Migration scripts never use semicolons as delimiters or in the column names.
    // Assume that the CSV uses semicolons as delimiters if any semicolon is found in the header line.
    if (line.contains(";")) {
        throw new IllegalArgumentException("Column names should be separated by commas, not semicolons")
    }

    return split(line)
}

static String serialize(Object obj, Size.Unit unitOverride) {
    switch (obj) {
        case null: return ""
        case String: {
            def result = obj.replace("\"", "\"\"")
            if (result.contains(",") || result.contains("\n") || result.contains("\r")) {
                result = "\"" + result + "\""
            }
            return result
        }
        case Double: return obj.toString()
        case Color: return obj.toHex()
        case Size: return obj.toString(unitOverride)
        case Tabs: return """{ "tabs": ${serialize(obj.tabs)}; "useOutsideTabs": "${serialize(obj.useOutsideTabs)}" }"""
        case Tab: return """{ "position": ${serialize(obj.position)}; "type": "${serialize(obj.type)}" }"""
        case List: return """[${obj.collect { serialize(it) }.join("; ")}]"""
        case Boolean: if (obj == true) {
            return "true"
        } else {
            return "false"
        }
        case { it.class.isEnum() }: return obj.toString()
        case LineSpacing: {
            def value = switch (obj) {
                case LineSpacing.MultipleOf -> serialize(obj.value)
                case LineSpacing.Additional -> serialize(obj.size)
                case LineSpacing.AtLeast -> serialize(obj.size)
                case LineSpacing.Exact -> serialize(obj.size)
                case LineSpacing.ExactFromPrevious -> serialize(obj.size)
                case LineSpacing.ExactFromPreviousWithAdjust -> serialize(obj.size)
                case LineSpacing.ExactFromPreviousWithAdjustLegacy -> serialize(obj.size)
                default -> throw new IllegalStateException("Unexpected LineSpacing value ${obj.toString()}")
            }
            return "${obj.class.simpleName},${value}"
        }
        default: throw new RuntimeException("Unexpected type for value ${obj.toString()}")
    }
}

static <T> T deserialize(String value, Class<T> cls) {
    if (value == null || value == "") {
        return null
    }

    switch (cls) {
        case String: return value as T
        case Color: return Color.fromHex(value) as T
        case Size: return Size.fromString(value) as T
        case Boolean:
        case boolean:
            if (value.toLowerCase() == "true") {
                return true as T
            } else if (value.toLowerCase() == "false") {
                return false as T
            } else {
                throw new RuntimeException("Invalid boolean value ${value}")
            }
        case { it.isEnum() }: {
            def enumValue = cls.find { it.toString().equalsIgnoreCase(value) }
            if (enumValue == null) {
                throw new RuntimeException("Invalid enum value ${value} for type ${cls}. Available options: ${cls.values().join(', ')}")
            }
            return enumValue as T
        }

        default: throw new RuntimeException("Unexpected type ${cls} for value ${value}")
    }
}

static String escapeJson(String value) {
    return value.replaceAll("[\n\r]", " ")
    .replaceAll("\"", "\\\"")
    .replaceAll(",", ";")
}