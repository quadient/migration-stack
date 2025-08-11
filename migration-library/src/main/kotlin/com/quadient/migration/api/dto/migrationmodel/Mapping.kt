package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.MappingItemEntity
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
    ) : MappingItem()

    data class Image(
        override var name: String?,
        var targetFolder: String?,
        var sourcePath: String?,
    ) : MappingItem()

    data class ParagraphStyle(override var name: String?, var definition: Definition?) : MappingItem() {
        sealed interface Definition
        data class Ref(val targetId: String) : Definition
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
        ) : Definition
    }

    data class TextStyle(override var name: String?, var definition: Definition?) : MappingItem() {
        sealed interface Definition
        data class Ref(val targetId: String) : Definition
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
        ) : Definition
    }

    data class Variable(
        override var name: String?,
        var dataType: DataType?,
        var inspirePath: String?,
    ) : MappingItem()

    fun toDb(): MappingItemEntity {
        return when (this) {
            is MappingItem.DocumentObject -> {
                MappingItemEntity.DocumentObject(
                    name = this.name,
                    internal = this.internal,
                    baseTemplate = this.baseTemplate,
                    targetFolder = this.targetFolder,
                    type = this.type
                )
            }

            is MappingItem.Image -> {
                MappingItemEntity.Image(
                    name = this.name, targetFolder = this.targetFolder, sourcePath = this.sourcePath
                )
            }

            is MappingItem.ParagraphStyle -> {
                val def = definition
                MappingItemEntity.ParagraphStyle(
                    name = this.name, definition = when (def) {
                        is MappingItem.ParagraphStyle.Ref -> MappingItemEntity.ParagraphStyle.Ref(
                            targetId = def.targetId
                        )

                        is MappingItem.ParagraphStyle.Def -> MappingItemEntity.ParagraphStyle.Def(
                            leftIndent = def.leftIndent,
                            rightIndent = def.rightIndent,
                            defaultTabSize = def.defaultTabSize,
                            spaceBefore = def.spaceBefore,
                            spaceAfter = def.spaceAfter,
                            alignment = def.alignment,
                            firstLineIndent = def.firstLineIndent,
                            lineSpacing = def.lineSpacing,
                            keepWithNextParagraph = def.keepWithNextParagraph,
                            tabs = def.tabs?.toDb()
                        )

                        null -> null
                    }
                )
            }

            is MappingItem.TextStyle -> {
                val def = definition
                MappingItemEntity.TextStyle(
                    name = this.name,

                    definition = when (def) {
                        is MappingItem.TextStyle.Ref -> MappingItemEntity.TextStyle.Ref(
                            targetId = def.targetId
                        )

                        is MappingItem.TextStyle.Def -> MappingItemEntity.TextStyle.Def(
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

                        null -> null
                    }
                )
            }

            is MappingItem.Variable -> {
                MappingItemEntity.Variable(
                    name = this.name,
                    dataType = dataType,
                    inspirePath = this.inspirePath,
                )
            }
        }
    }
}
