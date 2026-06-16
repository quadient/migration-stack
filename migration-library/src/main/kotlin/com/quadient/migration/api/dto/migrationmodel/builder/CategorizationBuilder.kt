package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.CategorizationMetadata
import com.quadient.migration.shared.CategorizationPrimitive
import java.time.Instant
import kotlin.time.Instant as KInstant

class CategorizationBuilder {
    private val fields: MutableList<CategorizationMetadata> = mutableListOf()

    companion object {
        /**
         * Creates a validity range pair from [start] and [end] instants.
         * @param start the start of the validity range (inclusive).
         * @param end the end of the validity range (inclusive).
         * @return a [Pair] representing the validity range.
         */
        @JvmStatic
        fun range(start: Instant, end: Instant): Pair<Instant, Instant> = start to end
    }

    /**
     * Adds a single boolean categorization field.
     *
     * @param key the categorization field key.
     * @param value the boolean value to associate with the key.
     * @return this builder instance for chaining.
     */
    fun bool(key: String, value: Boolean): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, listOf(CategorizationPrimitive.Bool(value))))
        return this
    }

    /**
     * Adds a boolean categorization field with multiple values.
     *
     * @param key the categorization field key.
     * @param values the boolean values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun bool(key: String, vararg values: Boolean): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map(CategorizationPrimitive::Bool).toList()))
        return this
    }

    /**
     * Adds a boolean categorization field with a collection of values.
     *
     * @param key the categorization field key.
     * @param values the collection of boolean values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun bool(key: String, values: Collection<Boolean>): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map(CategorizationPrimitive::Bool).toList()))
        return this
    }

    /**
     * Adds a single string categorization field.
     *
     * @param key the categorization field key.
     * @param value the string value to associate with the key.
     * @return this builder instance for chaining.
     */
    fun string(key: String, value: String): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, listOf(CategorizationPrimitive.Str(value))))
        return this
    }

    /**
     * Adds a string categorization field with multiple values.
     *
     * @param key the categorization field key.
     * @param values the string values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun string(key: String, vararg values: String): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map(CategorizationPrimitive::Str).toList()))
        return this
    }

    /**
     * Adds a string categorization field with a collection of values.
     *
     * @param key the categorization field key.
     * @param values the collection of string values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun string(key: String, values: Collection<String>): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map(CategorizationPrimitive::Str).toList()))
        return this
    }

    /**
     * Adds a single numeric categorization field.
     *
     * @param key the categorization field key.
     * @param value the numeric value to associate with the key.
     * @return this builder instance for chaining.
     */
    fun number(key: String, value: Double): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, listOf(CategorizationPrimitive.Number(value))))
        return this
    }

    /**
     * Adds a numeric categorization field with multiple values.
     *
     * @param key the categorization field key.
     * @param values the numeric values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun number(key: String, vararg values: Double): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map(CategorizationPrimitive::Number).toList()))
        return this
    }

    /**
     * Adds a numeric categorization field with a collection of values.
     *
     * @param key the categorization field key.
     * @param values the collection of numeric values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun number(key: String, values: Collection<Double>): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map(CategorizationPrimitive::Number).toList()))
        return this
    }

    /**
     * Adds a single date categorization field.
     *
     * @param key the categorization field key.
     * @param value the date value to associate with the key.
     * @return this builder instance for chaining.
     */
    fun date(key: String, value: Instant): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, listOf(CategorizationPrimitive.Date(value.toKInstant()))))
        return this
    }

    /**
     * Adds a date categorization field with multiple values.
     *
     * @param key the categorization field key.
     * @param values the date values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun date(key: String, vararg values: Instant): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map { CategorizationPrimitive.Date(it.toKInstant()) }.toList()))
        return this
    }

    /**
     * Adds a date categorization field with a collection of values.
     *
     * @param key the categorization field key.
     * @param values the collection of date values to associate with the key.
     * @return this builder instance for chaining.
     */
    fun date(key: String, values: Collection<Instant>): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map { CategorizationPrimitive.Date(it.toKInstant()) }.toList()))
        return this
    }

    /**
     * Adds a single validity range categorization field.
     *
     * @param key the categorization field key.
     * @param start the start of the validity range (inclusive).
     * @param end the end of the validity range (inclusive).
     * @return this builder instance for chaining.
     */
    fun validityRange(key: String, start: Instant, end: Instant): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, listOf(CategorizationPrimitive.ValidityRange(start.toKInstant(), end.toKInstant()))))
        return this
    }

    /**
     * Adds a validity range categorization field with multiple ranges.
     *
     * @param key the categorization field key.
     * @param values the validity ranges as pairs of start and end instants.
     * @return this builder instance for chaining.
     */
    fun validityRange(key: String, vararg values: Pair<Instant, Instant>): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map { (s, e) -> CategorizationPrimitive.ValidityRange(s.toKInstant(), e.toKInstant()) }))
        return this
    }

    /**
     * Adds a validity range categorization field with a collection of ranges.
     *
     * @param key the categorization field key.
     * @param values the collection of validity ranges as pairs of start and end instants.
     * @return this builder instance for chaining.
     */
    fun validityRange(key: String, values: Collection<Pair<Instant, Instant>>): CategorizationBuilder {
        fields.add(CategorizationMetadata(key, values.map { (s, e) -> CategorizationPrimitive.ValidityRange(s.toKInstant(), e.toKInstant()) }))
        return this
    }

    /**
     * Builds and returns the list of [CategorizationMetadata] entries added to this builder.
     *
     * @return the list of [CategorizationMetadata] entries.
     */
    fun build(): List<CategorizationMetadata> = fields
}

private fun Instant.toKInstant(): KInstant = KInstant.fromEpochSeconds(epochSecond, nano.toLong())