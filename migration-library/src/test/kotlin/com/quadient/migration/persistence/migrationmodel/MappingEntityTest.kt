package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.shared.millimeters
import com.quadient.migration.shared.points
import com.quadient.migration.tools.aParagraphStyle
import com.quadient.migration.tools.aParagraphStyleDefinition
import com.quadient.migration.tools.aTextStyle
import com.quadient.migration.tools.aTextStyleDefinition
import com.quadient.migration.tools.aVariable
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aVariableStructureModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MappingEntityTest {
    @Nested
    inner class DocumentObjectTest {
        @Test
        fun `nothing changes for document object with null mapping`() {
            val mapping = MappingItemEntity.DocumentObject(
                name = null,
                internal = null,
                baseTemplate = null,
                targetFolder = null,
                type = null,
            )
            val dto = DocumentObject.fromModel(
                aDocObj(
                    "doc1",
                    name = "Test doc",
                    internal = true,
                    baseTemplate = "base1",
                    targetFolder = "folder1",
                    type = DocumentObjectType.Section
                )
            )

            val result = mapping.apply(dto)

            assertEquals(result.name, "Test doc")
            assertEquals(result.internal, true)
            assertEquals(result.baseTemplate, "base1")
            assertEquals(result.targetFolder, "folder1")
            assertEquals(result.type, DocumentObjectType.Section)
        }

        @Test
        fun `everything changes for document object with null mapping`() {
            val mapping = MappingItemEntity.DocumentObject(
                name = "new name",
                internal = false,
                baseTemplate = "new base",
                targetFolder = "new folder",
                type = DocumentObjectType.Block,
            )
            val dto = DocumentObject.fromModel(
                aDocObj(
                    "doc1",
                    name = "Test doc",
                    internal = true,
                    baseTemplate = "base1",
                    targetFolder = "folder1",
                    type = DocumentObjectType.Section
                )
            )

            val result = mapping.apply(dto)

            assertEquals(result.name, "new name")
            assertEquals(result.internal, false)
            assertEquals(result.baseTemplate, "new base")
            assertEquals(result.targetFolder, "new folder")
            assertEquals(result.type, DocumentObjectType.Block)
        }
    }

    @Nested
    inner class ImageTest {
        @Test
        fun `nothing changes for image with null mapping`() {
            val mapping = MappingItemEntity.Image(
                name = null,
                targetFolder = null,
                sourcePath = null,
            )
            val dto = Image.fromModel(aImage("img1", name = "test img", targetFolder = "dir1", sourcePath = "source1"))

            val result = mapping.apply(dto)

            assertEquals(result.name, "test img")
            assertEquals(result.targetFolder, "dir1")
            assertEquals(result.sourcePath, "source1")
        }

        @Test
        fun `everything changes for image with null mapping`() {
            val mapping = MappingItemEntity.Image(
                name = "new name",
                targetFolder = "new folder",
                sourcePath = "new path",
            )
            val dto = Image.fromModel(aImage("img1", name = "test img", targetFolder = "dir1", sourcePath = "source1"))

            val result = mapping.apply(dto)

            assertEquals(result.name, "new name")
            assertEquals(result.targetFolder, "new folder")
            assertEquals(result.sourcePath, "new path")
        }
    }

    @Nested
    inner class ParagraphStyleTest {
        @Test
        fun `nothing changes for paragraph style with null mapping`() {
            val mapping = MappingItemEntity.ParagraphStyle(
                name = null,
                definition = null,
            )
            val refDto = aParagraphStyle("para1", name = "para1", definition = ParagraphStyleRef("other"))
            val defDto = aParagraphStyle(
                "para2", name = "para2", definition = aParagraphStyleDefinition(
                    defaultTabSize = 10.0.millimeters(),
                    leftIndent = 5.0.millimeters(),
                    rightIndent = 5.0.millimeters(),
                    spaceAfter = 10.0.millimeters(),
                    spaceBefore = 10.0.millimeters(),
                    alignment = Alignment.JustifyLeft,
                    firstLineIndent = 2.0.millimeters(),
                    lineSpacing = LineSpacing.ExactFromPrevious(12.0.millimeters()),
                    keepWithNextParagraph = true
                )
            )

            val refResult = mapping.apply(refDto)
            val defResult = mapping.apply(defDto)

            assertEquals(refResult.name, "para1")
            assertEquals(refResult.definition, ParagraphStyleRef("other"))

            assertEquals(defResult.name, "para2")
            assertEquals(defResult.definition, defDto.definition)
        }

        @Test
        fun `overrides with ref mapping`() {
            val mapping = MappingItemEntity.ParagraphStyle(
                name = "new name", definition = MappingItemEntity.ParagraphStyle.Ref("new other")
            )
            val dto = aParagraphStyle(
                "para1", name = "para1", definition = aParagraphStyleDefinition(
                    defaultTabSize = 10.0.millimeters(),
                    leftIndent = 5.0.millimeters(),
                    rightIndent = 5.0.millimeters(),
                    spaceAfter = 10.0.millimeters(),
                    spaceBefore = 10.0.millimeters(),
                    alignment = Alignment.JustifyLeft,
                    firstLineIndent = 2.0.millimeters(),
                    lineSpacing = LineSpacing.ExactFromPrevious(12.0.millimeters()),
                    keepWithNextParagraph = true
                )
            )

            val result = mapping.apply(dto)

            assertEquals(result.name, "new name")
            assertEquals(result.definition, ParagraphStyleRef("new other"))
        }

        @Test
        fun `overrides with def mapping`() {
            val mapping = MappingItemEntity.ParagraphStyle(
                name = "new name", definition = MappingItemEntity.ParagraphStyle.Def(
                    defaultTabSize = 15.0.millimeters(),
                    leftIndent = 6.0.millimeters(),
                    rightIndent = 6.0.millimeters(),
                    spaceAfter = 12.0.millimeters(),
                    spaceBefore = 12.0.millimeters(),
                    alignment = Alignment.JustifyCenter,
                    firstLineIndent = 3.0.millimeters(),
                    lineSpacing = LineSpacing.ExactFromPrevious(14.0.millimeters()),
                    keepWithNextParagraph = false,
                    tabs = TabsEntity(tabs = emptyList(), useOutsideTabs = true)
                )
            )
            val dto = aParagraphStyle(
                "para1", name = "para1", definition = aParagraphStyleDefinition(
                    defaultTabSize = 10.0.millimeters(),
                    leftIndent = 5.0.millimeters(),
                    rightIndent = 5.0.millimeters(),
                    spaceAfter = 10.0.millimeters(),
                    spaceBefore = 10.0.millimeters(),
                    alignment = Alignment.JustifyLeft,
                    firstLineIndent = 2.0.millimeters(),
                    lineSpacing = LineSpacing.ExactFromPrevious(12.0.millimeters()),
                    keepWithNextParagraph = true
                )
            )

            val result = mapping.apply(dto)
            val resultDef = result.definition
            if (resultDef !is ParagraphStyleDefinition ) {
                throw AssertionError("Expected ParagraphStyleDefinition, got ${resultDef::class.simpleName}")
            }

            assertEquals(result.name, "new name")
            assertEquals(resultDef.defaultTabSize, 15.0.millimeters())
            assertEquals(resultDef.leftIndent, 6.0.millimeters())
            assertEquals(resultDef.rightIndent, 6.0.millimeters())
            assertEquals(resultDef.spaceAfter, 12.0.millimeters())
            assertEquals(resultDef.spaceBefore, 12.0.millimeters())
            assertEquals(resultDef.alignment, Alignment.JustifyCenter)
            assertEquals(resultDef.firstLineIndent, 3.0.millimeters())
            assertEquals(resultDef.lineSpacing, LineSpacing.ExactFromPrevious(14.0.millimeters()))
            assertEquals(resultDef.keepWithNextParagraph, false)
            assertEquals(resultDef.tabs?.tabs?.size, 0)
            assertEquals(resultDef.tabs?.useOutsideTabs, true)
        }

        @Test
        fun `overrides ref with def mapping`() {
            val mapping = MappingItemEntity.ParagraphStyle(
                name = "new name", definition = MappingItemEntity.ParagraphStyle.Def(
                    defaultTabSize = 15.0.millimeters(),
                    leftIndent = 6.0.millimeters(),
                    rightIndent = 6.0.millimeters(),
                    spaceAfter = 12.0.millimeters(),
                    spaceBefore = 12.0.millimeters(),
                    alignment = Alignment.JustifyCenter,
                    firstLineIndent = 3.0.millimeters(),
                    lineSpacing = LineSpacing.ExactFromPrevious(14.0.millimeters()),
                    keepWithNextParagraph = false,
                    tabs = TabsEntity(tabs = emptyList(), useOutsideTabs = true)
                )
            )
            val dto = aParagraphStyle("para1", name = "para1", definition = ParagraphStyleRef("other"))

            val result = mapping.apply(dto)
            val resultDef = result.definition
            if (resultDef !is ParagraphStyleDefinition ) {
                throw AssertionError("Expected ParagraphStyleDefinition, got ${resultDef::class.simpleName}")
            }

            assertEquals(result.name, "new name")
            assertEquals(resultDef.defaultTabSize, 15.0.millimeters())
            assertEquals(resultDef.leftIndent, 6.0.millimeters())
            assertEquals(resultDef.rightIndent, 6.0.millimeters())
            assertEquals(resultDef.spaceAfter, 12.0.millimeters())
            assertEquals(resultDef.spaceBefore, 12.0.millimeters())
            assertEquals(resultDef.alignment, Alignment.JustifyCenter)
            assertEquals(resultDef.firstLineIndent, 3.0.millimeters())
            assertEquals(resultDef.lineSpacing, LineSpacing.ExactFromPrevious(14.0.millimeters()))
            assertEquals(resultDef.keepWithNextParagraph, false)
            assertEquals(resultDef.tabs?.tabs?.size, 0)
            assertEquals(resultDef.tabs?.useOutsideTabs, true)
        }
    }

    @Nested
    inner class TextStyleTest {
        @Test
        fun `nothing changes for text style with null mapping`() {
            val mapping = MappingItemEntity.TextStyle(
                name = null,
                definition = null,
            )
            val dto = aTextStyle("text1", name = "Text Style 1", definition = aTextStyleDefinition())

            val result = mapping.apply(dto)

            assertEquals(result.name, "Text Style 1")
            assertEquals(result.definition, dto.definition)
        }

        @Test
        fun `overrides with ref mapping`() {
            val mapping = MappingItemEntity.TextStyle(
                name = "new name", definition = MappingItemEntity.TextStyle.Ref("new other")
            )
            val dto = aTextStyle("text1", name = "Text Style 1", definition = aTextStyleDefinition())

            val result = mapping.apply(dto)

            assertEquals(result.name, "new name")
            assertEquals(result.definition, TextStyleRef("new other"))
        }

        @Test
        fun `overrides with def mapping`() {
            val mapping = MappingItemEntity.TextStyle(
                name = "new name", definition = MappingItemEntity.TextStyle.Def(
                    fontFamily = "New Font",
                    size = 12.0.points(),
                    foregroundColor = Color.fromHex("#FF0000"),
                    bold = true,
                    italic = false,
                    underline = false,
                    strikethrough = false,
                    superOrSubscript = SuperOrSubscript.Subscript,
                    interspacing = 1.0.points(),
                )
            )
            val dto = aTextStyle("text1", name = "Text Style 1", definition = aTextStyleDefinition())

            val result = mapping.apply(dto)
            val resultDef = result.definition
            if (resultDef !is TextStyleDefinition) {
                throw AssertionError("Expected TextStyleDefinition, got ${resultDef::class.simpleName}")
            }

            assertEquals(result.name, "new name")
            assertEquals(resultDef.fontFamily, "New Font")
            assertEquals(resultDef.size, 12.0.points())
            assertEquals(resultDef.foregroundColor, Color.fromHex("#FF0000"))
            assertEquals(resultDef.bold, true)
            assertEquals(resultDef.italic, false)
            assertEquals(resultDef.underline, false)
            assertEquals(resultDef.strikethrough, false)
            assertEquals(resultDef.superOrSubscript, SuperOrSubscript.Subscript)
            assertEquals(resultDef.interspacing, 1.0.points())
        }

        @Test
        fun `overrides ref with def mapping`() {
            val mapping = MappingItemEntity.TextStyle(
                name = "new name", definition = MappingItemEntity.TextStyle.Def(
                    fontFamily = "New Font",
                    size = 12.0.points(),
                    foregroundColor = Color.fromHex("#FF0000"),
                    bold = true,
                    italic = false,
                    underline = false,
                    strikethrough = false,
                    superOrSubscript = SuperOrSubscript.Subscript,
                    interspacing = 1.0.points(),
                )
            )
            val dto = aTextStyle("text1", name = "Text Style 1", definition = TextStyleRef("other"))

            val result = mapping.apply(dto)
            val resultDef = result.definition
            if (resultDef !is TextStyleDefinition) {
                throw AssertionError("Expected TextStyleDefinition, got ${resultDef::class.simpleName}")
            }

            assertEquals(result.name, "new name")
            assertEquals(resultDef.fontFamily, "New Font")
            assertEquals(resultDef.size, 12.0.points())
            assertEquals(resultDef.foregroundColor, Color.fromHex("#FF0000"))
            assertEquals(resultDef.bold, true)
            assertEquals(resultDef.italic, false)
            assertEquals(resultDef.underline, false)
            assertEquals(resultDef.strikethrough, false)
            assertEquals(resultDef.superOrSubscript, SuperOrSubscript.Subscript)
            assertEquals(resultDef.interspacing, 1.0.points())
        }
    }

    @Nested
    inner class VariableTest {
        @Test
        fun `nothing changes for variable with null mapping`() {
            val mapping = MappingItemEntity.Variable(
                name = null,
                dataType = null,
                inspirePath = null,
            )
            val dto = aVariable("var1", name = "Variable 1", dataType = DataType.String, defaultValue = "Default")
            val structure = VariableStructure.fromModel(aVariableStructureModel())

            val (resultVar, resultStructure) = mapping.apply(dto, structure)

            assertEquals(resultVar.name, "Variable 1")
            assertEquals(resultVar.dataType, DataType.String)
            assertEquals(resultStructure.structure.size, 0)
        }

        @Test
        fun `overrides with mapping`() {
            val mapping = MappingItemEntity.Variable(
                name = "new name",
                dataType = DataType.Double,
                inspirePath = "new/inspire/path",
            )
            val dto = aVariable("var1", name = "Variable 1", dataType = DataType.String, defaultValue = "Default")
            val structure = VariableStructure.fromModel(aVariableStructureModel())

            val (resultVar, resultStructure) = mapping.apply(dto, structure)

            assertEquals(resultVar.name, "new name")
            assertEquals(resultVar.dataType, DataType.Double)
            assertEquals(resultStructure.structure.size, 1)
            assertEquals(resultStructure.structure["var1"], "new/inspire/path")
        }
    }
}