package com.quadient.migration.example.common.util

import com.quadient.migration.api.dto.migrationmodel.Tab
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size

static String serialize(Object obj) {
    return serialize(obj, Size.Unit.Millimeters)
}

static Map<String, String> getCells(String line, String[] columnNames) {
    if (!line) return new HashMap<String, String>()

    def columns = line.split(",").toList()
    def values = new HashMap<String, String>()
    for (i in 0..<columnNames.length) {
        values.put(columnNames[i], columns[i])
    }

    return values
}

static String serialize(Object obj, Size.Unit unitOverride) {
    switch (obj) {
        case null: return ""
        case String: return obj
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
        case boolean: if (value.toLowerCase() == "true") {
            return true as T
        } else if (value.toLowerCase() == "false") {
            return false as T
        } else {
            throw new RuntimeException("Invalid boolean value ${value}")
        }
        case { it.isEnum() }: return cls.find { it.toString() == value} as T
        default: throw new RuntimeException("Unexpected type ${cls} for value ${value}")
    }
}