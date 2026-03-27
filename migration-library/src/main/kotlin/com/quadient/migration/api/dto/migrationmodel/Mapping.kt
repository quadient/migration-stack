package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.MappingItemEntity
import com.quadient.migration.persistence.migrationmodel.VariableEntityRef
import com.quadient.migration.shared.*

data class Mapping(val id: String, val mapping: MappingItem)

sealed class MappingItem {
    abstract var name: String?

    data class DocumentObject(
        override var name: String?,
        var internal: Boolean?,
        var baseTemplate: String?,
        var targetFolder: String?,
        var type: DocumentObjectType?,
        var variableStructureRef: String?,
        var skip: SkipOptions? = null,
    ) : MappingItem()

    data class Area(
        override var name: String?,
        var areas: MutableMap<Int, String?>,
        var flowToNextPage: MutableMap<Int, Boolean> = mutableMapOf(),
    ) : MappingItem()

    data class Image(
        override var name: String?,
        var targetFolder: String?,
        var sourcePath: String?,
        var imageType: ImageType?,
        var skip: SkipOptions? = null,
        var alternateText: String? = null,
        var targetAttachmentId: String? = null,
    ) : MappingItem()

    data class Attachment(
        override var name: String?,
        var targetFolder: String?,
        var sourcePath: String?,
        var attachmentType: AttachmentType?,
        var skip: SkipOptions? = null,
        var targetImageId: String? = null,
    ) : MappingItem()

    data class ParagraphStyle(
        override var name: String?,
        var targetId: String? = null,
        var definition: Def? = null
    ) : MappingItem() {
        data class Def(
            var leftIndent: Size?,
            var rightIndent: Size?,
            var defaultTabSize: Size?,
            var spaceBefore: Size?,
            var spaceAfter: Size?,
            var alignment: Alignment?,
            var firstLineIndent: Size?,
            var lineSpacing: LineSpacing?,
            var keepWithNextParagraph: Boolean?,
            var tabs: Tabs?,
            var pdfTaggingRule: ParagraphPdfTaggingRule?,
        )
    }

    data class TextStyle(
        override var name: String?,
        var targetId: String? = null,
        var definition: Def? = null
    ) : MappingItem() {
        data class Def(
            var fontFamily: String?,
            var foregroundColor: Color?,
            var size: Size?,
            var bold: Boolean?,
            var italic: Boolean?,
            var underline: Boolean?,
            var strikethrough: Boolean?,
            var superOrSubscript: SuperOrSubscript?,
            var interspacing: Size?,
        )
    }

    data class Variable(
        override var name: String?,
        var dataType: DataType?,
    ) : MappingItem()

    data class VariableStructure(
        override var name: String?,
        val mappings: MutableMap<String, VariablePathData>?,
        var languageVariable: VariableRef?,
    ) : MappingItem()

    data class DisplayRule(
        override var name: String?,
        var targetFolder: String?,
        var targetId: String?,
        var baseTemplate: String?,
        var variableStructureRef: String?,
        var internal: Boolean?,
    ) : MappingItem()

    fun toDb(): MappingItemEntity {
        return when (this) {
            is MappingItem.DocumentObject -> {
                MappingItemEntity.DocumentObject(
                    name = this.name,
                    internal = this.internal,
                    baseTemplate = this.baseTemplate,
                    targetFolder = this.targetFolder,
                    type = this.type,
                    variableStructureRef = this.variableStructureRef,
                    skip = this.skip,
                )
            }

            is MappingItem.Area -> MappingItemEntity.Area(
                name = this.name, areas = this.areas, flowToNextPage = this.flowToNextPage
            )

            is MappingItem.Image -> {
                MappingItemEntity.Image(
                    name = this.name,
                    targetFolder = this.targetFolder,
                    sourcePath = this.sourcePath,
                    imageType = this.imageType,
                    skip = this.skip,
                    alternateText = this.alternateText,
                    targetAttachmentId = this.targetAttachmentId,
                )
            }

            is MappingItem.Attachment -> {
                MappingItemEntity.Attachment(
                    name = this.name,
                    targetFolder = this.targetFolder,
                    sourcePath = this.sourcePath,
                    attachmentType = this.attachmentType,
                    skip = this.skip,
                    targetImageId = this.targetImageId,
                )
            }

            is MappingItem.ParagraphStyle -> {
                MappingItemEntity.ParagraphStyle(
                    name = this.name,
                    targetId = this.targetId,
                    definition = this.definition?.let { def ->
                        MappingItemEntity.ParagraphStyle.Def(
                            leftIndent = def.leftIndent,
                            rightIndent = def.rightIndent,
                            defaultTabSize = def.defaultTabSize,
                            spaceBefore = def.spaceBefore,
                            spaceAfter = def.spaceAfter,
                            alignment = def.alignment,
                            firstLineIndent = def.firstLineIndent,
                            lineSpacing = def.lineSpacing,
                            keepWithNextParagraph = def.keepWithNextParagraph,
                            tabs = def.tabs?.toDb(),
                            pdfTaggingRule = def.pdfTaggingRule
                        )
                    }
                )
            }

            is MappingItem.TextStyle -> {
                MappingItemEntity.TextStyle(
                    name = this.name,
                    targetId = this.targetId,
                    definition = this.definition?.let { def ->
                        MappingItemEntity.TextStyle.Def(
                            fontFamily = def.fontFamily,
                            foregroundColor = def.foregroundColor,
                            size = def.size,
                            bold = def.bold,
                            italic = def.italic,
                            underline = def.underline,
                            strikethrough = def.strikethrough,
                            superOrSubscript = def.superOrSubscript,
                            interspacing = def.interspacing
                        )
                    }
                )
            }

            is MappingItem.Variable -> {
                MappingItemEntity.Variable(
                    name = this.name,
                    dataType = dataType,
                )
            }

            is MappingItem.VariableStructure -> {
                MappingItemEntity.VariableStructure(
                    name = this.name,
                    mappings = mappings,
                    languageVariable = this.languageVariable?.let { VariableEntityRef(it.id) }
                )
            }

            is MappingItem.DisplayRule -> MappingItemEntity.DisplayRule(
                name = this.name,
                targetId = this.targetId,
                internal = this.internal,
                variableStructureRef = this.variableStructureRef,
                targetFolder = this.targetFolder,
                baseTemplate = this.baseTemplate,
            )
        }
    }
}
