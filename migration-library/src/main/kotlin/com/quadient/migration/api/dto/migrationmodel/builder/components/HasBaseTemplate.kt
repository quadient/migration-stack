package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateLocation
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.LiteralBaseTemplatePath

@Suppress("UNCHECKED_CAST")
interface HasBaseTemplate<T> {
    var baseTemplate: BaseTemplateLocation?

    /**
     * Override the default base template for this object.
     * @param baseTemplateLocation Location (literal path or base template reference) of the base template to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplate(baseTemplateLocation: BaseTemplateLocation?) =
        apply { this.baseTemplate = baseTemplateLocation } as T

    /**
     * Overrides the default base template for this object with a literal ICM path.
     * @param path Path to the base template to use for this object.
     * @return This builder instance for method chaining.
     */
    @Deprecated(
        message = "Use baseTemplatePath() for a literal path or baseTemplateRef() for a reference to a BaseTemplate migration object instead.",
        replaceWith = ReplaceWith("baseTemplatePath(baseTemplate)"),
    )
    fun baseTemplate(path: String?) = apply { this.baseTemplate = path?.let { LiteralBaseTemplatePath(it) } } as T

    /**
     * Overrides the default base template for this object with a literal ICM path.
     * @param path Path to the base template to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplatePath(path: String?) = apply { this.baseTemplate = path?.let { LiteralBaseTemplatePath(it) } } as T

    /**
     * Overrides the default base template for this object with a reference to a [BaseTemplate] migration object.
     * @param id ID of the [BaseTemplate] migration object to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplateRef(id: String?) = apply { this.baseTemplate = id?.let { BaseTemplateRef(it) } } as T

    /**
     * Overrides the default base template for this object with a reference to a [BaseTemplate] migration object.
     * @param baseTemplate The [BaseTemplate] migration object to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplateRef(baseTemplate: BaseTemplate) =
        apply { this.baseTemplate = BaseTemplateRef(baseTemplate.id) } as T
}
