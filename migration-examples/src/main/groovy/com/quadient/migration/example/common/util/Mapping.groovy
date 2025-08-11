package com.quadient.migration.example.common.util

static void mapProp(Object mapping, Object obj, String key, Object newValue) {
    if (newValue != null && newValue != obj[key] && mapping[key] != newValue && newValue != "") {
        mapping[key] = newValue
    }
}
