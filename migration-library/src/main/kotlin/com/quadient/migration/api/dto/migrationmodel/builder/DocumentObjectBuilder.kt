package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.PdfMetadata
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasBaseTemplate
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasCategorization
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasDocumentObjectOptions
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasEmailOptions
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSmsOptions
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasInternal
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasMetadata
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasPdfMetadata
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSkip
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasSubject
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasTargetFolder
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasVariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.api.dto.migrationmodel.EmailOptions
import com.quadient.migration.shared.MetadataEntry
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.api.dto.migrationmodel.SmsOptions

class DocumentObjectBuilder(id: String, private val type: DocumentObjectType) :
    DtoBuilderBase<DocumentObject, DocumentObjectBuilder>(id), DocumentContentBuilderBase<DocumentObjectBuilder>,
    HasDisplayRuleRef<DocumentObjectBuilder>,
    HasVariableStructureRef<DocumentObjectBuilder>,
    HasInternal<DocumentObjectBuilder>,
    HasTargetFolder<DocumentObjectBuilder>,
    HasBaseTemplate<DocumentObjectBuilder>,
    HasSubject<DocumentObjectBuilder>,
    HasDocumentObjectOptions<DocumentObjectBuilder>,
    HasMetadata<DocumentObjectBuilder>,
    HasCategorization<DocumentObjectBuilder>,
    HasPdfMetadata<DocumentObjectBuilder>,
    HasSkip<DocumentObjectBuilder>,
    HasAreaContent<DocumentObjectBuilder>,
    HasShapeContent<DocumentObjectBuilder>,
    HasBarcodeContent<DocumentContent, DocumentObjectBuilder>
{
    override val content: MutableList<DocumentContent> = mutableListOf()
    override var displayRuleRef: DisplayRuleRef? = null
    override var variableStructureRef: VariableStructureRef? = null
    override var internal: Boolean = false
    override var targetFolder: String? = null
    override var baseTemplate: String? = null
    override var subject: String? = null
    override var options: DocumentObjectOptions? = null
    override var metadata: MutableList<MetadataEntry> = mutableListOf()
    override var pdfMetadata: PdfMetadata? = null
    override var skip = false
    override var placeholder: String? = null
    override var reason: String? = null

    override fun build(): DocumentObject {
        return DocumentObject(
            id = id,
            type = type,
            name = name,
            content = content,
            internal = internal,
            targetFolder = targetFolder,
            originLocations = originLocations,
            customFields = customFields,
            displayRuleRef = displayRuleRef,
            variableStructureRef = variableStructureRef,
            baseTemplate = baseTemplate,
            options = options,
            pdfMetadata = pdfMetadata,
            metadata = metadata,
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            subject = subject,
        )
    }
}

class EmailObjectBuilder(id: String) : DtoBuilderBase<DocumentObject, EmailObjectBuilder>(id),
    HasParagraphContent<EmailObjectBuilder>,
    HasTableContent<DocumentContent, EmailObjectBuilder>,
    HasImageRefContent<DocumentContent, EmailObjectBuilder>,
    HasDocumentObjectRefContent<DocumentContent, EmailObjectBuilder>,
    HasFirstMatchContent<DocumentContent, EmailObjectBuilder>,
    HasSelectByLanguageContent<EmailObjectBuilder>,
    HasStringContent<DocumentContent, EmailObjectBuilder>,
    HasVariableRefContent<DocumentContent, EmailObjectBuilder>,
    HasRepeatedContent<EmailObjectBuilder>,
    HasBarcodeContent<DocumentContent, EmailObjectBuilder>,
    HasGridLayoutContent<DocumentContent, EmailObjectBuilder>,
    HasSkip<EmailObjectBuilder>,
    HasEmailOptions<EmailObjectBuilder>,
    HasVariableStructureRef<EmailObjectBuilder>
{
    override var variableStructureRef: VariableStructureRef? = null
    override val content: MutableList<DocumentContent> = mutableListOf()
    override var skip = false
    override var placeholder: String? = null
    override var reason: String? = null
    override var options: EmailOptions? = null

    override fun build(): DocumentObject {
        return DocumentObject(
            id = id,
            type = DocumentObjectType.Email,
            name = name,
            content = content,
            internal = true,
            targetFolder = null,
            originLocations = originLocations,
            customFields = customFields,
            displayRuleRef = null,
            variableStructureRef = variableStructureRef,
            baseTemplate = null,
            options = options,
            pdfMetadata = null,
            metadata = emptyList(),
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            subject = null,
        )
    }
}

class SmsObjectBuilder(id: String) : DtoBuilderBase<DocumentObject, SmsObjectBuilder>(id),
    HasStringContent<DocumentContent, SmsObjectBuilder>,
    HasVariableRefContent<DocumentContent, SmsObjectBuilder>,
    HasParagraphContent<SmsObjectBuilder>,
    HasSkip<SmsObjectBuilder>,
    HasSmsOptions<SmsObjectBuilder>,
    HasVariableStructureRef<SmsObjectBuilder>
{
    override var variableStructureRef: VariableStructureRef? = null
    override val content: MutableList<DocumentContent> = mutableListOf()
    override var skip = false
    override var placeholder: String? = null
    override var reason: String? = null
    override var options: SmsOptions? = null

    override fun build(): DocumentObject {
        return DocumentObject(
            id = id,
            type = DocumentObjectType.Sms,
            name = name,
            content = content,
            internal = true,
            targetFolder = null,
            originLocations = originLocations,
            customFields = customFields,
            displayRuleRef = null,
            variableStructureRef = variableStructureRef,
            baseTemplate = null,
            options = options,
            pdfMetadata = null,
            metadata = emptyList(),
            skip = SkipOptions(skipped = skip, reason = reason, placeholder = placeholder),
            subject = null,
        )
    }
}
