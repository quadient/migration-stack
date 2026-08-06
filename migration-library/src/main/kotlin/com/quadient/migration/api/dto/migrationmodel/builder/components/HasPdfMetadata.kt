package com.quadient.migration.api.dto.migrationmodel.builder.components

import com.quadient.migration.api.dto.migrationmodel.PdfMetadata
import com.quadient.migration.api.dto.migrationmodel.builder.PdfMetadataBuilder

@Suppress("UNCHECKED_CAST")
interface HasPdfMetadata<T> {
    var pdfMetadata: PdfMetadata?

    /**
     * Set PDF metadata for the object.
     * @param builder Builder function where receiver is a [PdfMetadataBuilder].
     * @return This builder instance for method chaining.
     */
    fun pdfMetadata(builder: PdfMetadataBuilder.() -> Unit) = apply {
        this.pdfMetadata = PdfMetadataBuilder().apply(builder).build()
    } as T

    /**
     * Set PDF metadata for the object.
     * @param pdfMetadata The [PdfMetadata] to set, or null to remove.
     * @return This builder instance for method chaining.
     */
    fun pdfMetadata(pdfMetadata: PdfMetadata?): T = apply {
        this.pdfMetadata = pdfMetadata
    } as T
}
