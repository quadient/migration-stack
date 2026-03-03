import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.mapping.ParagraphStylesImport
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size
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
    void createsPersistentDefMappingForNewStyle() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        mappingFile.toFile().write("""\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue,pdfTaggingRule
            new,newName,,[],1m,1m,1m,1m,1m,Center,1m,true,AtLeast,1m,
            """.stripIndent())
        when(migration.paragraphStyleRepository.find("new")).thenReturn(null)

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsert("new", new MappingItem.ParagraphStyle(
            "newName",
            null,
            new MappingItem.ParagraphStyle.Def(
                Size.ofMeters(1),
                Size.ofMeters(1),
                Size.ofMeters(1),
                Size.ofMeters(1),
                Size.ofMeters(1),
                Alignment.Center,
                Size.ofMeters(1),
                new LineSpacing.AtLeast(Size.ofMeters(1)),
                true,
                null,
                null
            )
        ))
        verify(migration.mappingRepository).applyParagraphStyleMapping("new")
    }

    @Test
    void createsPersistentMappingWithTargetIdForExistingStyle() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        mappingFile.toFile().write("""\
            id,name,targetId,originLocations,leftIndent,rightIndent,defaultTabSize,spaceBefore,spaceAfter,alignment,firstLineIndent,keepWithNextParagraph,lineSpacingType,lineSpacingValue,pdfTaggingRule
            existing,someNewName,otherRef,[],2m,2m,2m,2m,2m,Right,2m,false,AtLeast,2m,
            """.stripIndent())

        when(migration.paragraphStyleRepository.find("existing")).thenReturn(
            new ParagraphStyleBuilder("existing")
                .name("someName")
                .definition(new ParagraphStyleDefinitionBuilder().build())
                .build()
        )

        ParagraphStylesImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsert("existing", new MappingItem.ParagraphStyle(
            "someNewName",
            "otherRef",
            new MappingItem.ParagraphStyle.Def(
                Size.ofMeters(2),
                Size.ofMeters(2),
                Size.ofMeters(2),
                Size.ofMeters(2),
                Size.ofMeters(2),
                Alignment.Right,
                Size.ofMeters(2),
                new LineSpacing.AtLeast(Size.ofMeters(2)),
                false,
                null,
                null
            )
        ))
        verify(migration.mappingRepository).applyParagraphStyleMapping("existing")
    }
}
