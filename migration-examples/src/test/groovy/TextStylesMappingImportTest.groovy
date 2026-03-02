import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.example.common.mapping.TextStylesImport
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.argThat
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class TextStylesMappingImportTest {
    @TempDir
    java.io.File dir

    Migration migration

    @BeforeEach
    void setup() {
        migration = Utils.mockMigration()
    }

    @Test
    void createsPersistentDefMappingForNewStyle() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        mappingFile.toFile().write("""\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            new,newName,,[],MonoLisa,#2f2f2f,11pt,true,false,false,false,None,0.5pt
            """.stripIndent())
        when(migration.textStyleRepository.find("new")).thenReturn(null)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsert("new", new MappingItem.TextStyle(
            "newName",
            null,
            new MappingItem.TextStyle.Def(
                "MonoLisa",
                Color.fromHex("#2f2f2f"),
                Size.ofPoints(11),
                true,
                false,
                false,
                false,
                SuperOrSubscript.None,
                Size.ofPoints(0.5)
            )
        ))
        verify(migration.mappingRepository).applyTextStyleMapping("new")
    }

    @Test
    void createsPersistentRefMappingForExistingStyleAndBacksDefinitionUp() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        mappingFile.toFile().write("""\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            existing,someNewName,otherRef,[],CsvFont,#00ff00,13pt,false,false,false,false,Subscript,2pt
            """.stripIndent())

        when(migration.textStyleRepository.find("existing")).thenReturn(
            new TextStyleBuilder("existing")
                .name("someName")
                .definition(new TextStyleDefinitionBuilder().build())
                .build()
        )

        TextStylesImport.run(migration, mappingFile)

        verify(migration.textStyleRepository).upsert(argThat { style ->
            style.id == "existing" &&
                style.customFields["originalDefinition"]?.contains("\"fontFamily\":\"CsvFont\"")
        })
        verify(migration.mappingRepository).upsert("existing", new MappingItem.TextStyle("someNewName", "otherRef", null))
        verify(migration.mappingRepository).applyTextStyleMapping("existing")
    }
}
