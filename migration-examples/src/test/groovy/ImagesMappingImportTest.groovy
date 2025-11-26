import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.ImagesImport
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.*

class ImagesMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesImageName() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,status,originLocations
            unchanged,,,,,Active,[]
            kept,keptName,,,,Active,[]
            overridden,someName,,,,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", "someName", null, null, null)
        givenExistingImageMapping(migration, "kept", "keptName", null, null, null)
        givenExistingImage(migration, "overridden", "otherName", null, null, null)
        givenExistingImageMapping(migration, "overridden", null, null, null, null)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image("keptName", null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image("someName", null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesImageTargetFolder() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,status,originLocations
            unchanged,,,,,Active,[]
            kept,,,,keptFolder,Active,[]
            overridden,,,,overrideFolder,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", null, null, null, null)
        givenExistingImageMapping(migration, "kept", null, "keptFolder", null, null)
        givenExistingImage(migration, "overridden", null, "someFolder", null, null)
        givenExistingImageMapping(migration, "overridden", null, "overrideFolder", null, null)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, "keptFolder", null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, "overrideFolder", null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesImageSourcePath() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,status,originLocations
            unchanged,,,,,Active,[]
            kept,,keptPath,,,Active,[]
            overridden,,overridePath,,,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", null, null, "keptPath", null)
        givenExistingImageMapping(migration, "kept", null, null, "keptPath", null)
        givenExistingImage(migration, "overridden", null, null, "somePath", null)
        givenExistingImageMapping(migration, "overridden", null, null, "overridePath", null)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, "keptPath", null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, "overridePath", null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesImageType() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,status,originLocations
            unchanged,,,,,Active,[]
            kept,,,Jpeg,,Active,[]
            overridden,,,Gif,,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", null, null, null, ImageType.Jpeg)
        givenExistingImageMapping(migration, "kept", null, null, null, ImageType.Jpeg)
        givenExistingImage(migration, "overridden", null, null, null, ImageType.Png)
        givenExistingImageMapping(migration, "overridden", null, null, null, ImageType.Gif)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, null, ImageType.Jpeg, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, null, ImageType.Gif, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesSkipOptions() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,status,originLocation,skip,skipPlaceholder,skipReason
            unchanged,,,,,Active,[]
            kept,,,Jpeg,,Active,[]
            overridden,,,Gif,,Active,[],true,placeholder,reason
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", null, null, null, ImageType.Jpeg)
        givenExistingImageMapping(migration, "kept", null, null, null, ImageType.Jpeg)
        givenExistingImage(migration, "overridden", null, null, null, ImageType.Png)
        givenExistingImageMapping(migration, "overridden", null, null, null, ImageType.Gif)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, null, ImageType.Jpeg, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, null, ImageType.Gif, new SkipOptions(true, "placeholder", "reason")))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    static void givenExistingImage(Migration mig, String id, String name, String targetFolder, String sourcePath, ImageType imageType) {
        when(mig.imageRepository.find(id)).thenReturn(new Image(id, name, [], new CustomFieldMap([:]), sourcePath, null, imageType, targetFolder, [:], new SkipOptions(false, null, null)))
    }

    static void givenExistingImageMapping(Migration mig,
                                          String id,
                                          String name,
                                          String targetFolder,
                                          String sourcePath,
                                          ImageType imageType) {
        when(mig.mappingRepository.getImageMapping(id))
                .thenReturn(new MappingItem.Image(name, targetFolder, sourcePath, imageType, null))
    }
}
