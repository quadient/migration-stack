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
     * @param baseTemplate Location (literal path or base template reference) of the base template to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplate(baseTemplate: BaseTemplateLocation?) = apply { this.baseTemplate = baseTemplate } as T

    /**
     * Overrides the default base template for this object with a literal ICM path.
     * @param path Path to the base template to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplatePath(path: String?) = apply { this.baseTemplate = path?.let { LiteralBaseTemplatePath(it) } } as T

    /**
     * Overrides the default base template for this object with a reference to a [BaseTemplate] migration object.
     * @param baseTemplateId Id of the [BaseTemplate] migration object to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplateRef(baseTemplateId: String?) = apply { this.baseTemplate = baseTemplateId?.let { BaseTemplateRef(it) } } as T

    /**
     * Overrides the default base template for this object with a reference to a [BaseTemplate] migration object.
     * @param baseTemplate The [BaseTemplate] migration object to use for this object.
     * @return This builder instance for method chaining.
     */
    fun baseTemplateRef(baseTemplate: BaseTemplate) = apply { this.baseTemplate = BaseTemplateRef(baseTemplate.id) } as T
}
