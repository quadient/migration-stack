package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.data.TabsModel
import com.quadient.migration.persistence.table.MappingTable
import com.quadient.migration.shared.*
import com.quadient.migration.shared.LineSpacing.Additional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import com.quadient.migration.api.dto.migrationmodel.DocumentObject as DocumentObjectDto
import com.quadient.migration.api.dto.migrationmodel.Image as ImageDto
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle as ParagraphStyleDto
import com.quadient.migration.api.dto.migrationmodel.TextStyle as TextStyleDto
import com.quadient.migration.api.dto.migrationmodel.Variable as VariableDto
import com.quadient.migration.api.dto.migrationmodel.VariableStructure as VariableStructureDto

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
        @SerialName("documentObjectType") val type: DocumentObjectType?,
    ) : MappingItemEntity() {

        fun apply(item: DocumentObjectDto): DocumentObjectDto {
            return item.copy(
                name = name ?: item.name,
                internal = internal ?: item.internal,
                baseTemplate = baseTemplate ?: item.baseTemplate,
                targetFolder = targetFolder ?: item.targetFolder,
                type = type ?: item.type
            )
        }
    }

    @Serializable
    data class Image(
        override val name: String?,
        val targetFolder: String?,
        val sourcePath: String?,
    ) : MappingItemEntity() {
        fun apply(item: ImageDto): ImageDto {
            return item.copy(
                name = name ?: item.name,
                targetFolder = targetFolder ?: item.targetFolder,
                sourcePath = sourcePath ?: item.sourcePath
            )
        }
    }

    @Serializable
    data class ParagraphStyle(override val name: String?, val definition: Definition?) : MappingItemEntity() {
        @Serializable
        sealed interface Definition

        @Serializable
        data class Ref(val targetId: String) : Definition

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
            var tabs: TabsEntity?
        ) : Definition

        fun apply(item: ParagraphStyleDto): ParagraphStyleDto {
            return when (definition) {
                is Ref -> {
                    item.copy(
                        name = name ?: item.name, definition = ParagraphStyleRef(definition.targetId)
                    )
                }

                is Def -> {
                    val def = item.definition
                    when (def) {
                        is ParagraphStyleDefinition -> {
                            item.copy(
                                name = name ?: item.name, definition = def.copy(
                                    leftIndent = definition.leftIndent ?: def.leftIndent,
                                    rightIndent = definition.rightIndent ?: def.rightIndent,
                                    defaultTabSize = definition.defaultTabSize ?: def.defaultTabSize,
                                    spaceBefore = definition.spaceBefore ?: def.spaceBefore,
                                    spaceAfter = definition.spaceAfter ?: def.spaceAfter,
                                    alignment = definition.alignment ?: def.alignment,
                                    firstLineIndent = definition.firstLineIndent ?: def.firstLineIndent,
                                    lineSpacing = definition.lineSpacing ?: def.lineSpacing,
                                    keepWithNextParagraph = definition.keepWithNextParagraph
                                        ?: def.keepWithNextParagraph,
                                    tabs = definition.tabs?.let(TabsModel::fromDb)?.let(Tabs::fromModel) ?: def.tabs
                                )
                            )
                        }

                        is ParagraphStyleRef -> {
                            item.copy(
                                name = name ?: item.name, definition = ParagraphStyleDefinition(
                                    leftIndent = definition.leftIndent,
                                    rightIndent = definition.rightIndent,
                                    defaultTabSize = definition.defaultTabSize,
                                    spaceBefore = definition.spaceBefore,
                                    spaceAfter = definition.spaceAfter,
                                    alignment = definition.alignment ?: Alignment.Left,
                                    firstLineIndent = definition.firstLineIndent,
                                    lineSpacing = definition.lineSpacing ?: Additional(null),
                                    keepWithNextParagraph = definition.keepWithNextParagraph ?: false,
                                    tabs = definition.tabs?.let(TabsModel::fromDb)?.let(Tabs::fromModel)
                                )
                            )
                        }
                    }
                }

                null -> item
            }
        }
    }

    @Serializable
    data class Variable(
        override val name: String?,
        val dataType: DataType?,
        val inspirePath: String?,
    ) : MappingItemEntity() {
        fun apply(variable: VariableDto, structure: VariableStructureDto): Pair<VariableDto, VariableStructureDto> {
            val variable = variable.copy(
                name = name ?: variable.name,
                dataType = dataType ?: variable.dataType,
            )

            val structureDef = structure.structure.toMutableMap()
            val structure = structure.copy(
                structure = structureDef
            )

            if (!inspirePath.isNullOrEmpty()) {
                structureDef[variable.id] = inspirePath
            } else {
                structureDef.remove(variable.id)
            }

            return Pair(variable, structure)
        }
    }

    @Serializable
    data class TextStyle(override val name: String?, val definition: Definition?) : MappingItemEntity() {
        @Serializable
        sealed interface Definition

        @Serializable
        data class Ref(val targetId: String) : Definition

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
        ) : Definition

        fun apply(item: TextStyleDto): TextStyleDto {
            return when (definition) {
                is Ref -> {
                    item.copy(
                        name = name ?: item.name, definition = TextStyleRef(definition.targetId)
                    )
                }

                is Def -> {
                    val def = item.definition
                    when (def) {
                        is TextStyleDefinition -> {
                            item.copy(
                                name = name ?: item.name,
                                definition = def.copy(
                                    fontFamily = definition.fontFamily ?: def.fontFamily,
                                    foregroundColor = definition.foregroundColor ?: def.foregroundColor,
                                    size = definition.size ?: def.size,
                                    bold = definition.bold ?: def.bold,
                                    italic = definition.italic ?: def.italic,
                                    underline = definition.underline ?: def.underline,
                                    strikethrough = definition.strikethrough ?: def.strikethrough,
                                    superOrSubscript = definition.superOrSubscript ?: def.superOrSubscript,
                                    interspacing = definition.interspacing ?: def.interspacing
                                ),
                            )
                        }

                        is TextStyleRef -> {
                            item.copy(
                                name = name ?: item.name, definition = TextStyleDefinition(
                                    fontFamily = definition.fontFamily,
                                    foregroundColor = definition.foregroundColor,
                                    size = definition.size,
                                    bold = definition.bold ?: false,
                                    italic = definition.italic ?: false,
                                    underline = definition.underline ?: false,
                                    strikethrough = definition.strikethrough ?: false,
                                    superOrSubscript = definition.superOrSubscript ?: SuperOrSubscript.None,
                                    interspacing = definition.interspacing
                                )
                            )
                        }
                    }
                }

                null -> item
            }
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
                    type = this.type
                )
            }

            is Image -> {
                MappingItem.Image(
                    name = this.name, targetFolder = this.targetFolder, sourcePath = this.sourcePath
                )
            }

            is ParagraphStyle -> {
                MappingItem.ParagraphStyle(
                    name = this.name, definition = when (definition) {
                        is MappingItemEntity.ParagraphStyle.Def -> {
                            MappingItem.ParagraphStyle.Def(
                                leftIndent = definition.leftIndent,
                                rightIndent = definition.rightIndent,
                                defaultTabSize = definition.defaultTabSize,
                                spaceBefore = definition.spaceBefore,
                                spaceAfter = definition.spaceAfter,
                                alignment = definition.alignment,
                                firstLineIndent = definition.firstLineIndent,
                                lineSpacing = definition.lineSpacing,
                                keepWithNextParagraph = definition.keepWithNextParagraph,
                                tabs = definition.tabs?.let(TabsModel::fromDb)?.let(Tabs::fromModel)
                            )
                        }

                        is MappingItemEntity.ParagraphStyle.Ref -> {
                            MappingItem.ParagraphStyle.Ref(targetId = definition.targetId)
                        }

                        null -> null
                    }
                )
            }

            is TextStyle -> {
                MappingItem.TextStyle(
                    name = this.name, definition = when (definition) {
                        is MappingItemEntity.TextStyle.Def -> {
                            MappingItem.TextStyle.Def(
                                fontFamily = definition.fontFamily,
                                foregroundColor = definition.foregroundColor,
                                size = definition.size,
                                bold = definition.bold ?: false,
                                italic = definition.italic ?: false,
                                underline = definition.underline ?: false,
                                strikethrough = definition.strikethrough ?: false,
                                superOrSubscript = definition.superOrSubscript ?: SuperOrSubscript.None,
                                interspacing = definition.interspacing
                            )
                        }

                        is MappingItemEntity.TextStyle.Ref -> MappingItem.TextStyle.Ref(definition.targetId)

                        null -> null
                    }
                )
            }

            is Variable -> {
                MappingItem.Variable(
                    name = this.name, dataType = this.dataType, inspirePath = this.inspirePath
                )
            }
        }
    }
}
