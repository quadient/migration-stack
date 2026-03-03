package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.persistence.table.MappingTable
import com.quadient.migration.shared.*
import com.quadient.migration.shared.LineSpacing.Additional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import com.quadient.migration.api.dto.migrationmodel.DocumentObject as DocumentObjectModel
import com.quadient.migration.api.dto.migrationmodel.Image as ImageModel
import com.quadient.migration.api.dto.migrationmodel.Attachment as AttachmentModel
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle as ParagraphStyleModel
import com.quadient.migration.api.dto.migrationmodel.TextStyle as TextStyleModel
import com.quadient.migration.api.dto.migrationmodel.Variable as VariableModel
import com.quadient.migration.api.dto.migrationmodel.VariableStructure as VariableStructureModel
import com.quadient.migration.api.dto.migrationmodel.Area as AreaModel

class MappingEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<MappingEntity>(MappingTable)

    val resourceId by MappingTable.resourceId
    val type by MappingTable.type
    val projectName by MappingTable.projectName
    var mapping by MappingTable.mapping

    fun toDto(): Mapping {
        return Mapping(
            id = resourceId.value, mapping = mapping.toDto()
        )
    }
}

@Serializable
sealed class MappingItemEntity {
    abstract val name: String?

    @Serializable
    data class DocumentObject(
        override val name: String?,
        val internal: Boolean?,
        val baseTemplate: String?,
        val targetFolder: String?,
        val variableStructureRef: String?,
        @SerialName("documentObjectType") val type: DocumentObjectType?,
        var skip: SkipOptions? = null,
    ) : MappingItemEntity() {

        fun apply(item: DocumentObjectModel): DocumentObjectModel {
            return item.copy(
                name = name,
                internal = internal ?: false,
                baseTemplate = baseTemplate,
                targetFolder = targetFolder,
                type = type ?: item.type,
                variableStructureRef = variableStructureRef?.let { VariableStructureRef(it) },
                skip = skip ?: SkipOptions(false, null, null),
            )
        }
    }

    @Serializable
    data class Area(
        override val name: String?,
        val areas: MutableMap<Int, String?>,
        val flowToNextPage: MutableMap<Int, Boolean> = mutableMapOf()
    ) : MappingItemEntity() {
        fun apply(item: DocumentObjectModel): DocumentObjectModel {
            if (areas.isEmpty() && flowToNextPage.isEmpty()) {
                return item
            }

            var areaIndex = 0
            val updatedContent = item.content.map { contentItem ->
                if (contentItem is AreaModel) {
                    val idx = areaIndex++
                    contentItem.copy(
                        interactiveFlowName = if (areas.containsKey(idx)) areas[idx] else contentItem.interactiveFlowName,
                        flowToNextPage = if (flowToNextPage.containsKey(idx)) (flowToNextPage[idx] ?: false) else false
                    )
                } else {
                    contentItem
                }
            }
            return item.copy(content = updatedContent)
        }
    }

    @Serializable
    data class Image(
        override val name: String?,
        val targetFolder: String?,
        val sourcePath: String?,
        val imageType: ImageType? = null,
        var skip: SkipOptions? = null,
        val alternateText: String? = null,
        val targetAttachmentId: String? = null,
    ) : MappingItemEntity() {
        fun apply(item: ImageModel): ImageModel {
            return item.copy(
                name = name,
                targetFolder = targetFolder,
                sourcePath = sourcePath,
                imageType = imageType,
                skip = skip ?: SkipOptions(false, null, null),
                alternateText = alternateText,
                targetAttachmentId = targetAttachmentId,
            )
        }
    }

    @Serializable
    data class Attachment(
        override val name: String?,
        val targetFolder: String?,
        val sourcePath: String?,
        val attachmentType: AttachmentType?,
        var skip: SkipOptions? = null,
        val targetImageId: String? = null,
    ) : MappingItemEntity() {
        fun apply(item: AttachmentModel): AttachmentModel {
            return item.copy(
                name = name,
                targetFolder = targetFolder,
                sourcePath = sourcePath,
                attachmentType = attachmentType ?: item.attachmentType,
                skip = skip ?: SkipOptions(false, null, null),
                targetImageId = targetImageId,
            )
        }
    }

    @Serializable
    data class ParagraphStyle(
        override val name: String?,
        val targetId: String? = null,
        val definition: Def? = null
    ) : MappingItemEntity() {
        @Serializable
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
            var tabs: TabsEntity?,
            var pdfTaggingRule: ParagraphPdfTaggingRule?
        )

        fun apply(item: ParagraphStyleModel): ParagraphStyleModel {
            val updatedDefinition = definition?.let { def ->
                item.definition.copy(
                    leftIndent = def.leftIndent,
                    rightIndent = def.rightIndent,
                    defaultTabSize = def.defaultTabSize,
                    spaceBefore = def.spaceBefore,
                    spaceAfter = def.spaceAfter,
                    alignment = def.alignment ?: Alignment.Left,
                    firstLineIndent = def.firstLineIndent,
                    lineSpacing = def.lineSpacing ?: Additional(null),
                    keepWithNextParagraph = def.keepWithNextParagraph,
                    tabs = def.tabs?.let { tabsEntity ->
                        Tabs(
                            tabs = tabsEntity.tabs.map { Tab(it.position, it.type) },
                            useOutsideTabs = tabsEntity.useOutsideTabs
                        )
                    },
                    pdfTaggingRule = def.pdfTaggingRule
                )
            } ?: item.definition

            return item.copy(
                name = name,
                definition = updatedDefinition,
                targetId = targetId?.let { ParagraphStyleRef(it) }
            )
        }
    }

    @Serializable
    data class Variable(
        override val name: String?,
        val dataType: DataType?,
    ) : MappingItemEntity() {
        fun apply(variable: VariableModel): VariableModel {
            return variable.copy(
                name = name, dataType = dataType ?: variable.dataType,
            )
        }
    }

    @Serializable
    data class VariableStructure(
        override var name: String?,
        val mappings: MutableMap<String, VariablePathData>?,
        val languageVariable: VariableEntityRef?,
    ) : MappingItemEntity() {
        fun apply(item: VariableStructureModel): VariableStructureModel {
            return item.copy(
                name = name,
                structure = mappings ?: mutableMapOf(),
                languageVariable = languageVariable?.let { VariableRef(it.id) })
        }
    }


    @Serializable
    data class TextStyle(
        override val name: String?,
        val targetId: String? = null,
        val definition: Def? = null
    ) : MappingItemEntity() {
        @Serializable
        data class Def(
            var fontFamily: String?,
            var foregroundColor: Color?,
            var size: Size?,
            var bold: Boolean?,
            var italic: Boolean?,
            var underline: Boolean?,
            var strikethrough: Boolean?,
            var superOrSubscript: SuperOrSubscript?,
            var interspacing: Size?
        )

        fun apply(item: TextStyleModel): TextStyleModel {
            val updatedDefinition = definition?.let { def ->
                item.definition.copy(
                    fontFamily = def.fontFamily,
                    foregroundColor = def.foregroundColor,
                    size = def.size,
                    bold = def.bold ?: false,
                    italic = def.italic ?: false,
                    underline = def.underline ?: false,
                    strikethrough = def.strikethrough ?: false,
                    superOrSubscript = def.superOrSubscript ?: SuperOrSubscript.None,
                    interspacing = def.interspacing
                )
            } ?: item.definition

            return item.copy(
                name = name,
                definition = updatedDefinition,
                targetId = targetId?.let { TextStyleRef(it) },
            )
        }
    }

    fun toDto(): MappingItem {
        return when (this) {
            is DocumentObject -> {
                MappingItem.DocumentObject(
                    name = this.name,
                    internal = this.internal,
                    baseTemplate = this.baseTemplate,
                    targetFolder = this.targetFolder,
                    type = this.type,
                    variableStructureRef = this.variableStructureRef,
                    skip = this.skip
                )
            }

            is Area -> MappingItem.Area(name = this.name, areas = this.areas)

            is Image -> {
                MappingItem.Image(
                    name = this.name,
                    targetFolder = this.targetFolder,
                    sourcePath = this.sourcePath,
                    imageType = this.imageType,
                    skip = this.skip,
                    alternateText = this.alternateText,
                    targetAttachmentId = this.targetAttachmentId,
                )
            }

            is Attachment -> {
                MappingItem.Attachment(
                    name = this.name,
                    targetFolder = this.targetFolder,
                    sourcePath = this.sourcePath,
                    attachmentType = this.attachmentType,
                    skip = this.skip,
                    targetImageId = this.targetImageId,
                )
            }

            is ParagraphStyle -> {
                MappingItem.ParagraphStyle(
                    name = this.name,
                    targetId = this.targetId,
                    definition = this.definition?.let { def ->
                        MappingItem.ParagraphStyle.Def(
                            leftIndent = def.leftIndent,
                            rightIndent = def.rightIndent,
                            defaultTabSize = def.defaultTabSize,
                            spaceBefore = def.spaceBefore,
                            spaceAfter = def.spaceAfter,
                            alignment = def.alignment,
                            firstLineIndent = def.firstLineIndent,
                            lineSpacing = def.lineSpacing,
                            keepWithNextParagraph = def.keepWithNextParagraph,
                            tabs = def.tabs?.let { tabsEntity ->
                                Tabs(
                                    tabs = tabsEntity.tabs.map { Tab(it.position, it.type) },
                                    useOutsideTabs = tabsEntity.useOutsideTabs
                                )
                            },
                            pdfTaggingRule = def.pdfTaggingRule
                        )
                    }
                )
            }

            is TextStyle -> {
                MappingItem.TextStyle(
                    name = this.name,
                    targetId = this.targetId,
                    definition = this.definition?.let { def ->
                        MappingItem.TextStyle.Def(
                            fontFamily = def.fontFamily,
                            foregroundColor = def.foregroundColor,
                            size = def.size,
                            bold = def.bold ?: false,
                            italic = def.italic ?: false,
                            underline = def.underline ?: false,
                            strikethrough = def.strikethrough ?: false,
                            superOrSubscript = def.superOrSubscript ?: SuperOrSubscript.None,
                            interspacing = def.interspacing
                        )
                    }
                )
            }

            is Variable -> {
                MappingItem.Variable(name = this.name, dataType = this.dataType)
            }

            is VariableStructure -> {
                MappingItem.VariableStructure(
                    name = this.name,
                    mappings = this.mappings ?: mutableMapOf(),
                    languageVariable = this.languageVariable?.let { VariableRef(it.id) })
            }
        }
    }
}
