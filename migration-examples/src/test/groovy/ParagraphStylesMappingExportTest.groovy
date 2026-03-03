import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.mapping.ParagraphStylesExport
import com.quadient.migration.shared.Alignment
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
    File dir

    @Test
    void exportWorksCorrectlyForAllVariants() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        def emptyDefinition = new ParagraphStyleDefinitionBuilder()
            .additionalLineSpacing(Size.ofInches(0))
            .build()
        def fullDefinition = new ParagraphStyleDefinitionBuilder()
            .leftIndent(Size.ofInches(1))
            .rightIndent(Size.ofInches(1))
            .defaultTabSize(Size.ofInches(1))
            .spaceBefore(Size.ofInches(1))
            .spaceAfter(Size.ofInches(1))
            .alignment(Alignment.Center)
            .firstLineIndent(Size.ofInches(1))
            .additionalLineSpacing(Size.ofInches(1))
            .keepWithNextParagraph(true)
            .tabs(new Tabs([new Tab(Size.ofInches(1), TabType.Right)], true))
            .build()

        when(migration.paragraphStyleRepository.listAll()).thenReturn([
            new ParagraphStyleBuilder("empty").definition(emptyDefinition).build(),
            new ParagraphStyleBuilder("empty with targetId").definition(emptyDefinition).styleRef("other").build(),
            new ParagraphStyleBuilder("full").name("full").originLocations(["foo", "bar"]).definition(fullDefinition).build(),
            new ParagraphStyleBuilder("full with targetId").name("full").originLocations(["foo", "bar"]).definition(fullDefinition).styleRef("other").build(),
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
