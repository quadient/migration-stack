package com.quadient.migration.api.dto.migrationmodel.builder.components

@Suppress("UNCHECKED_CAST")
interface HasSubject<T> {
    var subject: String?

    /**
     * Sets the subject of the object. This is visible as a description in Interactive.
     * @param subject The subject of the document object, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun subject(subject: String?) = apply { this.subject = subject } as T }
