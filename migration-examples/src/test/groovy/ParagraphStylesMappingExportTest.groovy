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

import static org.mockito.Mockito.when

class ParagraphStylesMappingExportTest {
    @TempDir
    java.io.File dir

    @Test
    void exportWorksCorrectlyForAllVariants() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        def emptyDefinition = new ParagraphStyleDefinition(null, null, null, null, null, Alignment.Left, null, new LineSpacing.Additional(Size.ofInches(0)), null, null, null)
        def fullDefinition = new ParagraphStyleDefinition(Size.ofInches(1), Size.ofInches(1), Size.ofInches(1), Size.ofInches(1), Size.ofInches(1), Alignment.Center, Size.ofInches(1), new LineSpacing.Additional(Size.ofInches(1)), true, new Tabs([new Tab(Size.ofInches(1), TabType.Right)], true), null)

        when(migration.paragraphStyleRepository.listAll()).thenReturn([
            new ParagraphStyle("empty", null, [], new CustomFieldMap([:]), emptyDefinition),
            new ParagraphStyle("empty with targetId", null, [], new CustomFieldMap([:]), emptyDefinition, "other"),
            new ParagraphStyle("full", "full", ["foo", "bar"], new CustomFieldMap([:]), fullDefinition),
            new ParagraphStyle("full with targetId", "full", ["foo", "bar"], new CustomFieldMap([:]), fullDefinition, "other"),
        ])

        ParagraphStylesExport.run(migration, mappingFile)

        def expected = """\
            id,name,targetId,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue,pdfTaggingRule,originLocations (read-only)
            empty,,,,,,,,Left,,,Additional,0.0mm,,[]
            empty with targetId,,other,,,,,,Left,,,Additional,0.0mm,,[]
            full,full,,25.4mm,25.4mm,25.4mm,25.4mm,25.4mm,Center,25.4mm,true,Additional,25.4mm,,[foo; bar]
            full with targetId,full,other,25.4mm,25.4mm,25.4mm,25.4mm,25.4mm,Center,25.4mm,true,Additional,25.4mm,,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
