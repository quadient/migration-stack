import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.AttachmentsImport
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.*

class AttachmentsMappingImportTest {
    @TempDir
    File dir

    @Test
    void importsAttachmentMapping() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,attachmentType,targetFolder,status,originLocations,skip,skipPlaceholder,skipReason
            attachment1,newName,newPath,Attachment,newFolder,Active,[],false,,
            attachment2,,,Document,,Active,[],true,placeholder,reason
            """.stripIndent()
        mappingFile.toFile().write(input)
        
        givenExistingAttachment(migration, "attachment1", "oldName", "oldFolder", "oldPath", AttachmentType.Attachment)
        givenExistingAttachmentMapping(migration, "attachment1", "oldName", "oldFolder", "oldPath", AttachmentType.Attachment)
        givenExistingAttachment(migration, "attachment2", "someName", "someFolder", "somePath", AttachmentType.Document)
        givenExistingAttachmentMapping(migration, "attachment2", null, null, null, null)

        AttachmentsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("attachment1", new MappingItem.Attachment("newName", "newFolder", "newPath", AttachmentType.Attachment, new SkipOptions(false, null, null)))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("attachment1")
        verify(migration.mappingRepository, times(1)).upsert("attachment2", new MappingItem.Attachment(null, null, null, AttachmentType.Document, new SkipOptions(true, "placeholder", "reason")))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("attachment2")
    }

    static void givenExistingAttachment(Migration mig, String id, String name, String targetFolder, String sourcePath, AttachmentType attachmentType) {
        when(mig.attachmentRepository.find(id)).thenReturn(new Attachment(id, name, [], new CustomFieldMap([:]), sourcePath, targetFolder, attachmentType, new SkipOptions(false, null, null)))
    }

    static void givenExistingAttachmentMapping(Migration mig,
                                               String id,
                                               String name,
                                               String targetFolder,
                                               String sourcePath,
                                               AttachmentType attachmentType) {
        when(mig.mappingRepository.getAttachmentMapping(id))
                .thenReturn(new MappingItem.Attachment(name, targetFolder, sourcePath, attachmentType, null))
    }
}
