import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.ImagesExport
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class ImagesMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleImages() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.imageRepository.listAll()).thenReturn([
                new Image("empty", null, [], new CustomFieldMap([:]), null, null, null, null, [:], emptySkipOptions(), null, null),
                new Image("full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", null, ImageType.Jpeg, "targetDir", [:], new SkipOptions(true, "placeholder", "reason"), "Alt text for full", null),
                new Image("with-target-attachment", "with-target", [], new CustomFieldMap([:]), null, null, ImageType.Png, null, [:], emptySkipOptions(), null, "att123"),
                new Image("overridden empty", null, [], new CustomFieldMap([:]), null, null, null, null, [:], emptySkipOptions(), null, null),
                new Image("overridden full", "full", ["foo", "bar"], new CustomFieldMap(["originalName": "originalFull"]), "sourcePath", null, ImageType.Gif, "targetDir", [:], emptySkipOptions(), "Alt text for overridden", null),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        ImagesExport.run(migration, mappingFile)

        def expected = """\
            id,name,sourcePath,imageType,targetFolder,alternateText,targetAttachmentId,status,skip,skipPlaceholder,skipReason,originalName (read-only),originLocations (read-only)
            empty,,,,,,,Active,false,,,,[]
            full,full,sourcePath,Jpeg,targetDir,Alt text for full,,Active,true,placeholder,reason,,[foo; bar]
            with-target-attachment,with-target,,Png,,,att123,Active,false,,,,[]
            overridden empty,,,,,,,Active,false,,,,[]
            overridden full,full,sourcePath,Gif,targetDir,Alt text for overridden,,Active,false,,,originalFull,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    static SkipOptions emptySkipOptions() {
        return new SkipOptions(false, null, null)
    }
}
