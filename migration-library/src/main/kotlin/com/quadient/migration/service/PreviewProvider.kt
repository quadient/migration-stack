package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.Barcode
import com.quadient.migration.api.dto.migrationmodel.Code39Barcode
import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.GridLayout
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.QrCode
import com.quadient.migration.api.dto.migrationmodel.RepeatedContent
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.Shape
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.VariableRepository

class PreviewProvider(
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val variableRepository: VariableRepository,
) {
    fun getPreview(content: DocumentContent): String = content.toPreview(::resolveName)

    @JvmOverloads
    fun buildDocumentContentListPreview(content: List<DocumentContent>, limit: Int = 3): String {
        val parts = content.map { getPreview(it) }
        val taken = parts.take(limit)
        val suffix = if (parts.size > limit) listOf("(+${parts.size - limit} more)") else emptyList()
        return (taken + suffix).joinToString(";").replace(",", " ")
    }

    private fun resolveName(content: DocumentContent): String? = when (content) {
        is DocumentObjectRef -> documentObjectRepository.find(content.id)?.name
        is ImageRef -> imageRepository.find(content.id)?.name
        is AttachmentRef -> attachmentRepository.find(content.id)?.name
        is VariableRef -> variableRepository.find(content.id)?.name
        is StringValue -> null
        is Table -> null
        is Paragraph -> null
        is Area -> null
        is FirstMatch -> null
        is SelectByLanguage -> null
        is RepeatedContent -> null
        is ColumnLayout -> null
        is Shape -> null
        is QrCode -> null
        is Code39Barcode -> null
        is GridLayout -> null
    }
}
