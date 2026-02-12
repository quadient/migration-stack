import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.example.common.mapping.ImagesImport
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
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

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image("keptName", null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image("someName", null, null, null, new SkipOptions(false, null, null), null, null))
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

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, "keptFolder", null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, "overrideFolder", null, null, new SkipOptions(false, null, null), null, null))
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

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, "keptPath", null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, "overridePath", null, new SkipOptions(false, null, null), null, null))
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

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, null, ImageType.Jpeg, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, null, ImageType.Gif, new SkipOptions(false, null, null), null, null))
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

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, null, ImageType.Jpeg, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, null, ImageType.Gif, new SkipOptions(true, "placeholder", "reason"), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesAlternateText() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,alternateText,status,originLocations
            unchanged,,,,,,Active,[]
            kept,,,,,keptAltText,Active,[]
            overridden,,,,,overriddenAltText,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", null, null, null, null)
        givenExistingImageMapping(migration, "kept", null, null, null, null)
        givenExistingImage(migration, "overridden", null, null, null, null)
        givenExistingImageMapping(migration, "overridden", null, null, null, null)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), "keptAltText", null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), "overriddenAltText", null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesTargetAttachmentId() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,alternateText,targetAttachmentId,status,originLocations
            unchanged,,,,,,,Active,[]
            kept,,,,,,att123,Active,[]
            overridden,,,,,,att456,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null, null)
        givenExistingImage(migration, "kept", null, null, null, null)
        givenExistingImageMapping(migration, "kept", null, null, null, null)
        givenExistingImage(migration, "overridden", null, null, null, null)
        givenExistingImageMapping(migration, "overridden", null, null, null, null)

        ImagesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, "att123"))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, null, null, new SkipOptions(false, null, null), null, "att456"))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    static void givenExistingImage(Migration mig, String id, String name, String targetFolder, String sourcePath, ImageType imageType) {
        def image = new Image(id, name, [], new CustomFieldMap([:]), sourcePath, null, imageType, targetFolder, [:], new SkipOptions(false, null, null), null, null)
        when(mig.imageRepository.find(id)).thenReturn(image)
    }

    static void givenExistingImageMapping(Migration mig,
                                          String id,
                                          String name,
                                          String targetFolder,
                                          String sourcePath,
                                          ImageType imageType) {
        when(mig.mappingRepository.getImageMapping(id))
                .thenReturn(new MappingItem.Image(name, targetFolder, sourcePath, imageType, null, null, null))
    }

    @Test
    void createsNewImageWithStatus() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,imageType,targetFolder,alternateText,targetAttachmentId,status,originLocations,skip,skipPlaceholder,skipReason
            newImage1,ImageName,path/to/image.png,Png,ImageFolder,Alt text,att1,Active,[],false,,
            newImage2,AnotherImage,another/path.jpg,Jpeg,AnotherFolder,Another alt,,,[],false,,
            newImage3,DeployedImage,deployed/image.gif,Gif,,Deployed alt,att99,Deployed,[],true,skip-placeholder,skip-reason
            """.stripIndent()
        mappingFile.toFile().write(input)

        givenNewImage(migration, "newImage1")
        givenNewImageMapping(migration, "newImage1")
        givenNewImage(migration, "newImage2")
        givenNewImageMapping(migration, "newImage2")
        givenNewImage(migration, "newImage3")
        givenNewImageMapping(migration, "newImage3")

        ImagesImport.run(migration, mappingFile)

        verify(migration.imageRepository, times(3)).upsert(any(Image.class))
        verify(migration.statusTrackingRepository, times(1)).active(eq("newImage1"), eq(ResourceType.Image), any(Map.class))
        verify(migration.mappingRepository, times(1)).upsert("newImage1", new MappingItem.Image("ImageName", "ImageFolder", "path/to/image.png", ImageType.Png, new SkipOptions(false, null, null), "Alt text", "att1"))
        verify(migration.mappingRepository, times(1)).applyImageMapping("newImage1")

        verify(migration.statusTrackingRepository, times(1)).active(eq("newImage2"), eq(ResourceType.Image), any(Map.class))
        verify(migration.mappingRepository, times(1)).upsert("newImage2", new MappingItem.Image("AnotherImage", "AnotherFolder", "another/path.jpg", ImageType.Jpeg, new SkipOptions(false, null, null), "Another alt", null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("newImage2")

        verify(migration.statusTrackingRepository, times(1)).deployed(eq("newImage3"), anyString(), anyLong(), eq(ResourceType.Image), eq((String) null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
        verify(migration.mappingRepository, times(1)).upsert("newImage3", new MappingItem.Image("DeployedImage", null, "deployed/image.gif", ImageType.Gif, new SkipOptions(true, "skip-placeholder", "skip-reason"), "Deployed alt", "att99"))
        verify(migration.mappingRepository, times(1)).applyImageMapping("newImage3")
    }

    static void givenNewImage(Migration mig, String id) {
        def newImage = new ImageBuilder(id).build()
        when(mig.imageRepository.find(id)).thenReturn(null, newImage)
        when(mig.statusTrackingRepository.findLastEventRelevantToOutput(eq(id), any(), any())).thenReturn(null)
    }

    static void givenNewImageMapping(Migration mig, String id) {
        when(mig.mappingRepository.getImageMapping(id))
                .thenReturn(new MappingItem.Image(null, null, null, null, null, null, null))
    }
}
