package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.builder.EmailOptionsBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.SmsOptionsBuilder
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectOptions
import com.quadient.migration.api.dto.migrationmodel.EmailOptions
import com.quadient.migration.api.dto.migrationmodel.SmsOptions

@Suppress("UNCHECKED_CAST")
interface HasDocumentObjectOptions<T> {
    var options: DocumentObjectOptions?

    /**
     * Set options for the document object.
     * @param options [DocumentObjectOptions] to set for the document object.
     * @return This builder instance for method chaining.
     */
    fun options(options: DocumentObjectOptions?) = apply { this.options = options } as T
}

@Suppress("UNCHECKED_CAST")
interface HasEmailOptions<T> {
    var options: EmailOptions?

    /**
     * Set options for the email.
     * @param options [EmailOptions] to set for the email.
     * @return This builder instance for method chaining.
     */
    fun options(options: EmailOptions?) = apply { this.options = options } as T
    /**
     * Set options for the email using a builder.
     * @param builder Builder function where receiver is an [EmailOptionsBuilder].
     * @return This builder instance for method chaining.
     */
    fun options(builder: EmailOptionsBuilder.() -> Unit) = apply {
        this.options = EmailOptionsBuilder().apply(builder).build()
    } as T
}

@Suppress("UNCHECKED_CAST")
interface HasSmsOptions<T> {
    var options: SmsOptions?

    /**
     * Set options for the SMS.
     * @param options [SmsOptions] to set for the SMS.
     * @return This builder instance for method chaining.
     */
    fun options(options: SmsOptions?) = apply { this.options = options } as T

    /**
     * Set options for the SMS using a builder.
     * @param builder Builder function where receiver is a [SmsOptionsBuilder].
     * @return This builder instance for method chaining.
     */
    fun options(builder: SmsOptionsBuilder.() -> Unit) = apply {
        this.options = SmsOptionsBuilder().apply(builder).build()
    } as T
}
