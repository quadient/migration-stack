import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.TextStylesImport
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

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
    void createsNewStyleWithDefinition() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            new,newName,,[foo; bar],MonoLisa,#2f2f2f,11pt,true,true,true,true,Superscript,0.5pt
            """.stripIndent()
        mappingFile.toFile().write(input)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.textStyleRepository).upsert(new TextStyle("new", "newName", [], new CustomFieldMap([:]),
            new TextStyleDefinition("MonoLisa", Color.fromHex("#2f2f2f"), Size.ofPoints(11), true, true, true, true, SuperOrSubscript.Superscript, Size.ofPoints(0.5))))
    }

    @Test
    void createsNewStyleWithRef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            new,newName,otherStyle,[foo; bar],,,,,,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.textStyleRepository).upsert(new TextStyle("new", "newName", [], new CustomFieldMap([:]),
            new TextStyleRef("otherStyle")))
    }

    @Test
    void remapsExistingRefStyleToDef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            existing,someNewName,,[foo; bar],MonoLisa,#2f2f2f,11pt,true,true,true,true,Superscript,0.5pt
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingRefTextStyle("existing", "someName", "someRef")
        givenExistingNoDefTextStyleMapping("existing", null)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.TextStyle("someNewName",
                new MappingItem.TextStyle.Def("MonoLisa",
                    Color.fromHex("#2f2f2f"),
                    Size.ofPoints(11),
                    true,
                    true,
                    true,
                    true,
                    SuperOrSubscript.Superscript,
                    Size.ofPoints(0.5))))
        verify(migration.mappingRepository).applyTextStyleMapping("existing")
    }

    @Test
    void remapsExistingRefStyleToRef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            existing,someNewName,otherRef,[foo; bar],,,,,,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingRefTextStyle("existing", "someName", "someRef")
        givenExistingNoDefTextStyleMapping("existing", null)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.TextStyle("someNewName", new MappingItem.TextStyle.Ref("otherRef")))
        verify(migration.mappingRepository).applyTextStyleMapping("existing")
    }

    @Test
    void remapsExistingDefStyleToRef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            existing,someNewName,otherRef,[foo; bar],,,,,,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDefinitionTextStyle("existing", "someName", "MonoLisa", "#2f2f2f", 11.0, true, true, true, true, SuperOrSubscript.Superscript, 0.5)
        givenExistingNoDefTextStyleMapping("existing", null)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.TextStyle("someNewName", new MappingItem.TextStyle.Ref("otherRef")))
        verify(migration.mappingRepository).applyTextStyleMapping("existing")
    }

    @Test
    void remapsExistingDefStyleToDef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            existing,someNewName,,[foo; bar],Roboto,#ff0000,12pt,true,false,true,false,Subscript,1pt
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDefinitionTextStyle("existing", "someName", "MonoLisa", "#2f2f2f", 11.0, true, true, true, true, SuperOrSubscript.Superscript, 0.5)
        givenExistingNoDefTextStyleMapping("existing", null)

        TextStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.TextStyle("someNewName",
                new MappingItem.TextStyle.Def("Roboto",
                    Color.fromHex("#ff0000"),
                    Size.ofPoints(12),
                    true,
                    false,
                    true,
                    false,
                    SuperOrSubscript.Subscript,
                    Size.ofPoints(1))))
        verify(migration.mappingRepository).applyTextStyleMapping("existing")
    }

    void givenExistingRefTextStyle(String id, String name, String ref) {
        when(migration.textStyleRepository.find(id)).thenReturn(new TextStyle(id, name, [], new CustomFieldMap([:]), new TextStyleRef(ref)))
    }

    void givenExistingDefinitionTextStyle(String id,
                                          String name,
                                          String fontFamily,
                                          String hexColor,
                                          Double fontSizePt,
                                          Boolean bold,
                                          Boolean italic,
                                          Boolean underline,
                                          Boolean strikethrough,
                                          SuperOrSubscript superOrSubscript,
                                          Double interspacingPt) {
        when(migration.textStyleRepository.find(id)).thenReturn(new TextStyle(id, name, [], new CustomFieldMap([:]),
            new TextStyleDefinition(fontFamily,
                Color.fromHex(hexColor),
                Size.ofPoints(fontSizePt),
                bold,
                italic,
                underline,
                strikethrough,
                superOrSubscript,
                Size.ofPoints(interspacingPt))))
    }

    void givenExistingNoDefTextStyleMapping(String id, String name) {
        when(migration.mappingRepository.getTextStyleMapping(id)).thenReturn(new MappingItem.TextStyle(name, null))
    }

    void givenExistingRefTextStyleMapping(String id, String name, String targetId) {
        when(migration.mappingRepository.getTextStyleMapping(id))
            .thenReturn(new MappingItem.TextStyle(name, new MappingItem.TextStyle.Ref(targetId)))
    }

    void givenExistingDefinitionTextStyleMapping(String id,
                                                 String name,
                                                 String fontFamily,
                                                 String hexColor,
                                                 Double fontSizePt,
                                                 Boolean bold,
                                                 Boolean italic,
                                                 Boolean underline,
                                                 Boolean strikethrough,
                                                 SuperOrSubscript superOrSubscript,
                                                 Double interspacingPt) {
        when(migration.mappingRepository.getTextStyleMapping(id))
            .thenReturn(new MappingItem.TextStyle(name, new MappingItem.TextStyle.Def(fontFamily,
                hexColor != null ? Color.fromHex(hexColor) : null,
                fontSizePt != null ? Size.ofPoints(fontSizePt) : null,
                bold,
                italic,
                underline,
                strikethrough,
                superOrSubscript,
                interspacingPt != null ? Size.ofPoints(interspacingPt) : null)))
    }
}