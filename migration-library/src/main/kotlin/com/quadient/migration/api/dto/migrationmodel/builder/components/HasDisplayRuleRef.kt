package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef

@Suppress("UNCHECKED_CAST")
interface HasDisplayRuleRef<T> {
    var displayRuleRef: DisplayRuleRef?

    /**
     * Sets the display rule reference for conditional display.
     * @param displayRuleRef The display rule reference to set, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun displayRuleRef(displayRuleRef: DisplayRuleRef?) = apply { this.displayRuleRef = displayRuleRef } as T
    @Deprecated("Use displayRuleRef instead. This function will be removed in a future version.")
    fun displayRule(displayRuleRef: DisplayRuleRef?) = apply { this.displayRuleRef = displayRuleRef } as T

    /**
     * Sets the display rule reference for conditional display using a string ID.
     * @param displayRuleRefId The ID of the display rule to reference, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun displayRuleRef(displayRuleRefId: String?) = apply { this.displayRuleRef = displayRuleRefId?.let { DisplayRuleRef(it) } } as T
    @Deprecated("Use displayRuleRef instead. This function will be removed in a future version.")
    fun displayRule(displayRuleRefId: String?) = apply { this.displayRuleRef = displayRuleRefId?.let { DisplayRuleRef(it) } } as T

    /**
     * Sets the display rule reference for conditional display using a [DisplayRule] model object.
     * @param rule The display rule whose ID will be used as the reference, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun displayRuleRef(rule: DisplayRule?) = apply { this.displayRuleRef = rule?.let { DisplayRuleRef(it.id) } } as T
    @Deprecated("Use displayRuleRef instead. This function will be removed in a future version.")
    fun displayRule(rule: DisplayRule?) = apply { this.displayRuleRef = rule?.let { DisplayRuleRef(it.id) } } as T
}
