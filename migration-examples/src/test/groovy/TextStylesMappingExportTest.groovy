import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.example.common.mapping.TextStylesExport
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.when

class TextStylesMappingExportTest {
    @TempDir
    java.io.File dir

    @Test
    void exportWorksCorrectlyForAllVariants() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        def emptyDefinition = new TextStyleDefinitionBuilder().build()
        def fullDefinition = new TextStyleDefinitionBuilder()
            .fontFamily("Arial")
            .foregroundColor("#FF0000")
            .size(Size.ofInches(1))
            .bold(true)
            .italic(true)
            .underline(true)
            .strikethrough(true)
            .superOrSubscript(SuperOrSubscript.Superscript)
            .interspacing(Size.ofCentimeters(1))
            .build()

        when(migration.textStyleRepository.listAll()).thenReturn([
            new TextStyleBuilder("empty").definition(emptyDefinition).build(),
            new TextStyleBuilder("empty with targetId").definition(emptyDefinition).styleRef("other").build(),
            new TextStyleBuilder("full").name("full").originLocations(["foo", "bar"]).definition(fullDefinition).build(),
            new TextStyleBuilder("full with targetId").name("full").originLocations(["foo", "bar"]).definition(fullDefinition).styleRef("other").build(),
        ])

        TextStylesExport.run(migration, mappingFile)

        def expected = """\
            id,name,targetId,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing,originLocations (read-only)
            empty,,,,,,false,false,false,false,None,,[]
            empty with targetId,,other,,,,false,false,false,false,None,,[]
            full,full,,Arial,#ff0000,1in,true,true,true,true,Superscript,1cm,[foo; bar]
            full with targetId,full,other,Arial,#ff0000,1in,true,true,true,true,Superscript,1cm,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
