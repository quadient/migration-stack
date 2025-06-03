package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MigrationObject

@Suppress("UNCHECKED_CAST")
abstract class DtoBuilderBase<T : MigrationObject, K : DtoBuilderBase<T, K>>(
    protected val id: String
) {
    init {
        require(id.isNotEmpty()) { "id cannot be null or empty" }
    }

    protected var name: String? = null
    protected var originLocations: List<String> = emptyList()
    protected var customFields = CustomFieldMap()

    /**
     * Sets the name of the object. If not provided the name will default to the id.
     * @param name The name to set.
     * @return The builder instance for method chaining.
     */
    fun name(name: String): K {
        this.name = name
        return this as K
    }

    /**
     * Sets the custom fields for the object.
     * @param customFields A map of custom field key-value pairs.
     * @return The builder instance for method chaining.
     */
    fun customFields(customFields: Map<String, String>): K {
        this.customFields = CustomFieldMap(customFields.toMutableMap())
        return this as K
    }

    /**
     * Adds a custom field to the object.
     * @param key The key for the custom field.
     * @param value The value for the custom field.
     * @param condition If true, the custom field will be added; if false, it will not be added.
     * @return The builder instance for method chaining.
     */
    fun addCustomField(key: String, value: String?, condition: Boolean? = true): K {
        if (condition != true) return this as K
        require(value != null) { "Custom field value cannot be null if condition evaluates to true" }

        customFields.put(key, value)
        return this as K
    }

    /**
     * Adds a custom field to the object.
     * @param key The key for the custom field.
     * @param value The value for the custom field.
     * @return The builder instance for method chaining.
     */
    fun addCustomField(key: String, value: String): K {
        customFields.put(key, value)
        return this as K
    }

    /**
     * Sets the origin locations for the object.
     * @param originLocations A list of origin locations.
     * @return The builder instance for method chaining.
     */
    fun originLocations(originLocations: List<String>): K {
        this.originLocations = originLocations
        return this as K
    }

    abstract fun build(): T
}