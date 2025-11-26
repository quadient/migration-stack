import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.ParagraphStylesExport
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TabType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class ParagraphStylesMappingExportTest {
    @TempDir
    File dir

    @Test
    void exportWorksCorrectlyForAllVariants() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        def emptyDefinition = new ParagraphStyleDefinition(null, null, null, null, null, Alignment.Left, null, new LineSpacing.Additional(Size.ofInches(0)), null, null)
        def fullDefinition = new ParagraphStyleDefinition(Size.ofInches(1), Size.ofInches(1), Size.ofInches(1), Size.ofInches(1), Size.ofInches(1), Alignment.Center, Size.ofInches(1), new LineSpacing.Additional(Size.ofInches(1)), true, new Tabs([new Tab(Size.ofInches(1), TabType.Right)], true))

        def overrideMappingDef = new MappingItem.ParagraphStyle("newName", new MappingItem.ParagraphStyle.Def(Size.ofInches(2), Size.ofInches(2), Size.ofInches(2), Size.ofInches(2), Size.ofInches(2), Alignment.Right, Size.ofInches(2), new LineSpacing.ExactFromPrevious(Size.ofInches(2)), true, new Tabs([new Tab(Size.ofInches(2), TabType.Right)], true)))
        def overrideMappingRef = new MappingItem.ParagraphStyle("newName", new MappingItem.ParagraphStyle.Ref("new other"))

        when(migration.paragraphStyleRepository.listAll()).thenReturn([
            new ParagraphStyle("empty", null, [], new CustomFieldMap([:]), emptyDefinition),
            new ParagraphStyle("empty with targetId", null, [], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
            new ParagraphStyle("full", "full", ["foo", "bar"], new CustomFieldMap([:]), fullDefinition),
            new ParagraphStyle("full with targetId", "full", ["foo", "bar"], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
            new ParagraphStyle("empty overridden by def", null, [], new CustomFieldMap([:]), emptyDefinition),
            new ParagraphStyle("empty overridden by ref", null, [], new CustomFieldMap([:]), emptyDefinition),
            new ParagraphStyle("empty with targetId overridden by def", null, [], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
            new ParagraphStyle("empty with targetId overridden by ref", null, [], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
            new ParagraphStyle("full overridden by def", null, [], new CustomFieldMap([:]), fullDefinition),
            new ParagraphStyle("full overridden by ref", null, [], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
            new ParagraphStyle("full with targetId overridden by def", null, [], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
            new ParagraphStyle("full with targetId overridden by ref", null, [], new CustomFieldMap([:]), new ParagraphStyleRef("other")),
        ])

        when(migration.mappingRepository.getParagraphStyleMapping(any())).thenReturn(new MappingItem.ParagraphStyle(null, null))
        when(migration.mappingRepository.getParagraphStyleMapping("empty overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getParagraphStyleMapping("empty overridden by ref")).thenReturn(overrideMappingRef)
        when(migration.mappingRepository.getParagraphStyleMapping("empty with targetId overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getParagraphStyleMapping("empty with targetId overridden by ref")).thenReturn(overrideMappingRef)
        when(migration.mappingRepository.getParagraphStyleMapping("full overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getParagraphStyleMapping("full overridden by ref")).thenReturn(overrideMappingRef)
        when(migration.mappingRepository.getParagraphStyleMapping("full with targetId overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getParagraphStyleMapping("full with targetId overridden by ref")).thenReturn(overrideMappingRef)

        ParagraphStylesExport.run(migration, mappingFile)

        "id,name,targetId,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue,originLocations (read-only)"

        def expected = """\
            id,name,targetId,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue,originLocations (read-only)
            empty,,,,,,,,Left,,,Additional,0.0mm,[]
            empty with targetId,,other,,,,,,,,,,[]
            full,full,,25.4mm,25.4mm,25.4mm,25.4mm,25.4mm,Center,25.4mm,true,Additional,25.4mm,[foo; bar]
            full with targetId,full,other,,,,,,,,,,[foo; bar]
            empty overridden by def,,,,,,,,Left,,,Additional,0.0mm,[]
            empty overridden by ref,,,,,,,,Left,,,Additional,0.0mm,[]
            empty with targetId overridden by def,,other,,,,,,,,,,[]
            empty with targetId overridden by ref,,other,,,,,,,,,,[]
            full overridden by def,,,25.4mm,25.4mm,25.4mm,25.4mm,25.4mm,Center,25.4mm,true,Additional,25.4mm,[]
            full overridden by ref,,other,,,,,,,,,,[]
            full with targetId overridden by def,,other,,,,,,,,,,[]
            full with targetId overridden by ref,,other,,,,,,,,,,[]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
