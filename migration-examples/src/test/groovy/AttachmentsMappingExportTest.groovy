import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.AttachmentsExport
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class AttachmentsMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleFiles() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.attachmentRepository.listAll()).thenReturn([
                new Attachment("empty", null, [], new CustomFieldMap([:]), null, null, AttachmentType.Document, emptySkipOptions()),
                new Attachment("full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", "targetDir", AttachmentType.Document, new SkipOptions(true, "placeholder", "reason")),
                new Attachment("overridden empty", null, [], new CustomFieldMap([:]), null, null, AttachmentType.Document, emptySkipOptions()),
                new Attachment("overridden full", "full", ["foo", "bar"], new CustomFieldMap(["originalName": "originalFull"]), "sourcePath", "targetDir", AttachmentType.Attachment, emptySkipOptions()),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        AttachmentsExport.run(migration, mappingFile)

        def expected = """\
            id,name,sourcePath,attachmentType,targetFolder,status,skip,skipPlaceholder,skipReason,originalName (read-only),originLocations (read-only)
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
