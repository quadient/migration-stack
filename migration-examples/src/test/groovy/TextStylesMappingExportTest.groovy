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

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class TextStylesMappingExportTest {
    @TempDir
    File dir

    @Test
    void exportWorksCorrectlyForAllVariants() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        def emptyDefinition = new TextStyleDefinition(null, null, null, false, false, false, false, SuperOrSubscript.None, null)
        def fullDefinition = new TextStyleDefinition("Arial", Color.fromHex("#FF0000"), Size.ofInches(1), true, true, true, true, SuperOrSubscript.Superscript, Size.ofCentimeters(1))

        def overrideMappingDef = new MappingItem.TextStyle("newName", new MappingItem.TextStyle.Def("Iosevka", Color.fromHex("#00FF00"), Size.ofInches(2), true, true, true, true, SuperOrSubscript.Subscript, Size.ofCentimeters(2)))
        def overrideMappingRef = new MappingItem.TextStyle("newName", new MappingItem.TextStyle.Ref("new other"))

        when(migration.textStyleRepository.listAll()).thenReturn([
            new TextStyle("empty", null, [], new CustomFieldMap([:]), emptyDefinition),
            new TextStyle("empty with targetId", null, [], new CustomFieldMap([:]), new TextStyleRef("other")),
            new TextStyle("full", "full", ["foo", "bar"], new CustomFieldMap([:]), fullDefinition),
            new TextStyle("full with targetId", "full", ["foo", "bar"], new CustomFieldMap([:]), new TextStyleRef("other")),
            new TextStyle("empty overridden by def", null, [], new CustomFieldMap([:]), emptyDefinition),
            new TextStyle("empty overridden by ref", null, [], new CustomFieldMap([:]), emptyDefinition),
            new TextStyle("empty with targetId overridden by def", null, [], new CustomFieldMap([:]), new TextStyleRef("other")),
            new TextStyle("empty with targetId overridden by ref", null, [], new CustomFieldMap([:]), new TextStyleRef("other")),
            new TextStyle("full overridden by def", null, [], new CustomFieldMap([:]), fullDefinition),
            new TextStyle("full overridden by ref", null, [], new CustomFieldMap([:]), new TextStyleRef("other")),
            new TextStyle("full with targetId overridden by def", null, [], new CustomFieldMap([:]), new TextStyleRef("other")),
            new TextStyle("full with targetId overridden by ref", null, [], new CustomFieldMap([:]), new TextStyleRef("other")),
        ])

        when(migration.mappingRepository.getTextStyleMapping(any())).thenReturn(new MappingItem.TextStyle(null, null))
        when(migration.mappingRepository.getTextStyleMapping("empty overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getTextStyleMapping("empty overridden by ref")).thenReturn(overrideMappingRef)
        when(migration.mappingRepository.getTextStyleMapping("empty with targetId overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getTextStyleMapping("empty with targetId overridden by ref")).thenReturn(overrideMappingRef)
        when(migration.mappingRepository.getTextStyleMapping("full overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getTextStyleMapping("full overridden by ref")).thenReturn(overrideMappingRef)
        when(migration.mappingRepository.getTextStyleMapping("full with targetId overridden by def")).thenReturn(overrideMappingDef)
        when(migration.mappingRepository.getTextStyleMapping("full with targetId overridden by ref")).thenReturn(overrideMappingRef)

        TextStylesExport.run(migration, mappingFile)

        def expected = """\
            id,name,targetId,origin_locations,fontFamily,foregroundColor,size,bold,italic,underline,strikethrough,superOrSubscript,interspacing
            empty,,,[],,,,false,false,false,false,None,
            empty with targetId,,other,[],,,,,,,,,
            full,full,,[foo; bar],Arial,#ff0000,72.0pt,true,true,true,true,Superscript,10.0mm
            full with targetId,full,other,[foo; bar],,,,,,,,,
            empty overridden by def,,,[],,,,false,false,false,false,None,
            empty overridden by ref,,,[],,,,false,false,false,false,None,
            empty with targetId overridden by def,,other,[],,,,,,,,,
            empty with targetId overridden by ref,,other,[],,,,,,,,,
            full overridden by def,,,[],Arial,#ff0000,72.0pt,true,true,true,true,Superscript,10.0mm
            full overridden by ref,,other,[],,,,,,,,,
            full with targetId overridden by def,,other,[],,,,,,,,,
            full with targetId overridden by ref,,other,[],,,,,,,,,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
