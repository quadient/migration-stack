package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.tools.computeIfPresentOrPut

typealias InnerMap = MutableMap<String, String>
data class CustomFieldMap(val inner: InnerMap  = mutableMapOf()) : InnerMap by inner {
    fun appendUnique(key: String, addedValue: String) {
        this.computeIfPresentOrPut(key, addedValue) { existingValue ->
            existingValue
                .split(",")
                .toMutableList()
                .also { it.add(addedValue) }
                .distinct()
                .joinToString(",")
        }
    }

    fun getArray(key: String): List<String>? {
        return this[key]?.split(",")
    }
}