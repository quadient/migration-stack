import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.FilesExport
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class FilesMappingExportTest {
    @TempDir
    java.io.File dir

    @Test
    void allPossibleFiles() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.fileRepository.listAll()).thenReturn([
                new File("empty", null, [], new CustomFieldMap([:]), null, null, FileType.Document, emptySkipOptions()),
                new File("full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", "targetDir", FileType.Document, new SkipOptions(true, "placeholder", "reason")),
                new File("overridden empty", null, [], new CustomFieldMap([:]), null, null, FileType.Document, emptySkipOptions()),
                new File("overridden full", "full", ["foo", "bar"], new CustomFieldMap(["originalName": "originalFull"]), "sourcePath", "targetDir", FileType.Attachment, emptySkipOptions()),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        FilesExport.run(migration, mappingFile)

        def expected = """\
            id,name,sourcePath,fileType,targetFolder,status,skip,skipPlaceholder,skipReason,originalName (read-only),originLocations (read-only)
            empty,,,Document,,Active,false,,,,[]
            full,full,sourcePath,Document,targetDir,Active,true,placeholder,reason,,[foo; bar]
            overridden empty,,,Document,,Active,false,,,,[]
            overridden full,full,sourcePath,Attachment,targetDir,Active,false,,,originalFull,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    static SkipOptions emptySkipOptions() {
        return new SkipOptions(false, null, null)
    }
}
