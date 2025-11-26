import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.ParagraphStylesImport
import com.quadient.migration.shared.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class ParagraphStylesMappingImportTest {
    @TempDir
    File dir

    Migration migration

    @BeforeEach
    void setup() {
        migration = Utils.mockMigration()
    }

    @Test
    void createsNewStyleWithDefinition() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            new,newName,,[foo; bar],1m,1m,1m,1m,1m,Center,1m,true,Exact,
            """.stripIndent()
        mappingFile.toFile().write(input)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.paragraphStyleRepository).upsert(new ParagraphStyle("new", "newName", [], new CustomFieldMap([:]),
            new ParagraphStyleDefinition(Size.ofMeters(1),
                Size.ofMeters(1),
                Size.ofMeters(1),
                Size.ofMeters(1),
                Size.ofMeters(1),
                Alignment.Center,
                Size.ofMeters(1),
                new LineSpacing.Exact(null),
                true,
                null)))
    }

    @Test
    void createsNewStyleWithRef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            new,newName,otherStyle,[foo; bar],,,,,,,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.paragraphStyleRepository).upsert(new ParagraphStyle("new", "newName", [], new CustomFieldMap([:]),
            new ParagraphStyleRef("otherStyle")))
    }

    @Test
    void remapsExistingRefStyleToDef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            existing,someNewName,,[foo; bar],1m,1m,1m,1m,1m,Center,1m,true,AtLeast,1m
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingRefStyle("existing", "someName", "someRef")
        givenExistingNoDefStyleMapping("existing", null)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.ParagraphStyle("someNewName",
                new MappingItem.ParagraphStyle.Def(Size.ofMeters(1),
                    Size.ofMeters(1),
                    Size.ofMeters(1),
                    Size.ofMeters(1),
                    Size.ofMeters(1),
                    Alignment.Center,
                    Size.ofMeters(1),
                    new LineSpacing.AtLeast(Size.ofMeters(1)),
                    true,
                    null)))
        verify(migration.mappingRepository).applyParagraphStyleMapping("existing")
    }

    @Test
    void remapsExistingRefStyleToRef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            existing,someNewName,otherRef,[foo; bar],,,,,,,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingRefStyle("existing", "someName", "someRef")
        givenExistingNoDefStyleMapping("existing", null)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.ParagraphStyle("someNewName", new MappingItem.ParagraphStyle.Ref("otherRef")))
        verify(migration.mappingRepository).applyParagraphStyleMapping("existing")
    }

    @Test
    void remapsExistingDefStyleToRef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            existing,someNewName,otherRef,[foo; bar],,,,,,,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDefinitionStyle("existing", "someName", 1, 1, 1, 1, 1, Alignment.Center, 1, new LineSpacing.Exact(Size.ofMeters(1)), true)
        givenExistingNoDefStyleMapping("existing", null)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.ParagraphStyle("someNewName", new MappingItem.ParagraphStyle.Ref("otherRef")))
        verify(migration.mappingRepository).applyParagraphStyleMapping("existing")
    }

    @Test
    void remapsExistingDefStyleToDef() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            existing,someNewName,,[foo; bar],2m,2m,2m,2m,2m,Right,2m,false,AtLeast,2m
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDefinitionStyle("existing", "someName", 1, 1, 1, 1, 1, Alignment.Center, 1, new LineSpacing.Exact(Size.ofMeters(1)), true)
        givenExistingNoDefStyleMapping("existing", null)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.ParagraphStyle("someNewName",
                new MappingItem.ParagraphStyle.Def(Size.ofMeters(2),
                    Size.ofMeters(2),
                    Size.ofMeters(2),
                    Size.ofMeters(2),
                    Size.ofMeters(2),
                    Alignment.Right,
                    Size.ofMeters(2),
                    new LineSpacing.AtLeast(Size.ofMeters(2)),
                    false,
                    null)))
        verify(migration.mappingRepository).applyParagraphStyleMapping("existing")
    }

    @Test
    void adjustsOnlySomePropertiesInDefToDefMapping() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def input = """\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue
            existing,someNewName,,[foo; bar],1m,1m,1m,2m,2m,Right,2m,true,Exact,1m
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDefinitionStyle("existing", "someName", 1, 1, 1, 1, 1, Alignment.Center, 1, new LineSpacing.Exact(Size.ofMeters(1)), true)
        givenExistingDefinitionStyleMapping("existing", "someName", 1, 1, null, null, null, null, 1, null, null)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository)
            .upsert("existing", new MappingItem.ParagraphStyle("someNewName",
                new MappingItem.ParagraphStyle.Def(Size.ofMeters(1),
                    Size.ofMeters(1),
                    null,
                    Size.ofMeters(2),
                    Size.ofMeters(2),
                    Alignment.Right,
                    Size.ofMeters(2),
                    null,
                    null,
                    null)))
        verify(migration.mappingRepository).applyParagraphStyleMapping("existing")
    }

    void givenExistingRefStyle(String id, String name, String ref) {
        when(migration.paragraphStyleRepository.find(id))
            .thenReturn(new ParagraphStyle(id, name, [], new CustomFieldMap([:]), new ParagraphStyleRef(ref)))
    }

    void givenExistingDefinitionStyle(String id,
                                      String name,
                                      Double leftIndentM,
                                      Double rightIndentM,
                                      Double defaultTabSizeM,
                                      Double spaceBeforeM,
                                      Double spaceAfterM,
                                      Alignment alignment,
                                      Double firstLineIndentM,
                                      LineSpacing lineSpacing,
                                      Boolean keepWithNextParagraph) {
        when(migration.paragraphStyleRepository.find(id)).thenReturn(new ParagraphStyle(id, name, [], new CustomFieldMap([:]),
            new ParagraphStyleDefinition(leftIndentM != null ? Size.ofMeters(leftIndentM) : null,
                rightIndentM != null ? Size.ofMeters(rightIndentM) : null,
                defaultTabSizeM != null ? Size.ofMeters(defaultTabSizeM) : null,
                spaceBeforeM != null ? Size.ofMeters(spaceBeforeM) : null,
                spaceAfterM != null ? Size.ofMeters(spaceAfterM) : null,
                alignment,
                firstLineIndentM != null ? Size.ofMeters(firstLineIndentM) : null,
                lineSpacing,
                keepWithNextParagraph,
                null)))
    }

    void givenExistingNoDefStyleMapping(String id, String name) {
        when(migration.mappingRepository.getParagraphStyleMapping(id)).thenReturn(new MappingItem.ParagraphStyle(name, null))
    }

    void givenExistingRefStyleMapping(String id, String name, String targetId) {
        when(migration.mappingRepository.getParagraphStyleMapping(id))
            .thenReturn(new MappingItem.ParagraphStyle(name, new MappingItem.ParagraphStyle.Ref(targetId)))
    }

    void givenExistingDefinitionStyleMapping(String id,
                                             String name,
                                             Double leftIndentM,
                                             Double rightIndentM,
                                             Double defaultTabSizeM,
                                             Double spaceBeforeM,
                                             Double spaceAfterM,
                                             Alignment alignment,
                                             Double firstLineIndentM,
                                             LineSpacing lineSpacing,
                                             Boolean keepWithNextParagraph) {
        when(migration.mappingRepository.getParagraphStyleMapping(id))
            .thenReturn(new MappingItem.ParagraphStyle(name, new MappingItem.ParagraphStyle.Def(leftIndentM != null ? Size.ofMeters(leftIndentM) : null,
                rightIndentM != null ? Size.ofMeters(rightIndentM) : null,
                defaultTabSizeM != null ? Size.ofMeters(defaultTabSizeM) : null,
                spaceBeforeM != null ? Size.ofMeters(spaceBeforeM) : null,
                spaceAfterM != null ? Size.ofMeters(spaceAfterM) : null,
                alignment,
                firstLineIndentM != null ? Size.ofMeters(firstLineIndentM) : null,
                lineSpacing,
                keepWithNextParagraph,
                null)))
    }
}
