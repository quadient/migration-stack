import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.TextStylesExport
import com.quadient.migration.shared.Color
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
        def emptyDefinition = new TextStyleDefinition(null, null, null, false, false, false, false, SuperOrSubscript.None, null)
        def fullDefinition = new TextStyleDefinition("Arial", Color.fromHex("#FF0000"), Size.ofInches(1), true, true, true, true, SuperOrSubscript.Superscript, Size.ofCentimeters(1))

        when(migration.textStyleRepository.listAll()).thenReturn([
            new TextStyle("empty", null, [], new CustomFieldMap([:]), emptyDefinition),
            new TextStyle("empty with targetId", null, [], new CustomFieldMap([:]), emptyDefinition, "other"),
            new TextStyle("full", "full", ["foo", "bar"], new CustomFieldMap([:]), fullDefinition),
            new TextStyle("full with targetId", "full", ["foo", "bar"], new CustomFieldMap([:]), fullDefinition, "other"),
        ])

        TextStylesExport.run(migration, mappingFile)

        def expected = """\
            id,name,targetId,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing,originLocations (read-only)
            empty,,,,,,false,false,false,false,None,,[]
            empty with targetId,,other,,,,false,false,false,false,None,,[]
            full,full,,Arial,#ff0000,72.0pt,true,true,true,true,Superscript,10.0mm,[foo; bar]
            full with targetId,full,other,Arial,#ff0000,72.0pt,true,true,true,true,Superscript,10.0mm,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
