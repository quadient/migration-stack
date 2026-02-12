import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.example.common.mapping.AttachmentsImport
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class AttachmentsMappingImportTest {
    @TempDir
    File dir

    @Test
    void importsAttachmentMapping() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,attachmentType,targetFolder,targetImageId,status,originLocations,skip,skipPlaceholder,skipReason
            attachment1,newName,newPath,Attachment,newFolder,img123,Active,[],false,,
            attachment2,,,Document,,,Active,[],true,placeholder,reason
            """.stripIndent()
        mappingFile.toFile().write(input)
        
        givenExistingAttachment(migration, "attachment1", "oldName", "oldFolder", "oldPath", AttachmentType.Attachment)
        givenExistingAttachmentMapping(migration, "attachment1", "oldName", "oldFolder", "oldPath", AttachmentType.Attachment)
        givenExistingAttachment(migration, "attachment2", "someName", "someFolder", "somePath", AttachmentType.Document)
        givenExistingAttachmentMapping(migration, "attachment2", null, null, null, null)

        AttachmentsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("attachment1", new MappingItem.Attachment("newName", "newFolder", "newPath", AttachmentType.Attachment, new SkipOptions(false, null, null), "img123"))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("attachment1")
        verify(migration.mappingRepository, times(1)).upsert("attachment2", new MappingItem.Attachment(null, null, null, AttachmentType.Document, new SkipOptions(true, "placeholder", "reason"), null))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("attachment2")
    }

    static void givenExistingAttachment(Migration mig, String id, String name, String targetFolder, String sourcePath, AttachmentType attachmentType) {
        def attachment = new Attachment(id, name, [], new CustomFieldMap([:]), sourcePath, targetFolder, attachmentType, new SkipOptions(false, null, null), null)
        when(mig.attachmentRepository.find(id)).thenReturn(attachment)
    }

    static void givenExistingAttachmentMapping(Migration mig,
                                               String id,
                                               String name,
                                               String targetFolder,
                                               String sourcePath,
                                               AttachmentType attachmentType) {
        when(mig.mappingRepository.getAttachmentMapping(id))
                .thenReturn(new MappingItem.Attachment(name, targetFolder, sourcePath, attachmentType, null, null))
    }

    @Test
    void createsNewAttachments() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,attachmentType,targetFolder,targetImageId,status,originLocations,skip,skipPlaceholder,skipReason
            newAttachment1,AttachmentName,path/to/file.pdf,Document,MyFolder,img1,Active,[],false,,
            newAttachment2,AnotherAttachment,another/path.doc,Attachment,AnotherFolder,,,[],false,,
            newAttachment3,DeployedAttachment,deployed/file.pdf,Document,,img99,Deployed,[],true,skip-placeholder,skip-reason
            """.stripIndent()
        mappingFile.toFile().write(input)

        givenNewAttachment(migration, "newAttachment1")
        givenNewAttachmentMapping(migration, "newAttachment1")
        givenNewAttachment(migration, "newAttachment2")
        givenNewAttachmentMapping(migration, "newAttachment2")
        givenNewAttachment(migration, "newAttachment3")
        givenNewAttachmentMapping(migration, "newAttachment3")

        AttachmentsImport.run(migration, mappingFile)

        verify(migration.attachmentRepository, times(3)).upsert(any(Attachment.class))

        verify(migration.statusTrackingRepository, times(1)).active(eq("newAttachment1"), eq(ResourceType.Attachment), any(Map.class))
        verify(migration.mappingRepository, times(1)).upsert("newAttachment1", new MappingItem.Attachment("AttachmentName", "MyFolder", "path/to/file.pdf", AttachmentType.Document, new SkipOptions(false, null, null), "img1"))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("newAttachment1")

        verify(migration.statusTrackingRepository, times(1)).active(eq("newAttachment2"), eq(ResourceType.Attachment), any(Map.class))
        verify(migration.mappingRepository, times(1)).upsert("newAttachment2", new MappingItem.Attachment("AnotherAttachment", "AnotherFolder", "another/path.doc", AttachmentType.Attachment, new SkipOptions(false, null, null), null))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("newAttachment2")

        verify(migration.statusTrackingRepository, times(1)).deployed(eq("newAttachment3"), anyString(), anyLong(), eq(ResourceType.Attachment), eq((String)null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
        verify(migration.mappingRepository, times(1)).upsert("newAttachment3", new MappingItem.Attachment("DeployedAttachment", null, "deployed/file.pdf", AttachmentType.Document, new SkipOptions(true, "skip-placeholder", "skip-reason"), "img99"))
        verify(migration.mappingRepository, times(1)).applyAttachmentMapping("newAttachment3")
    }

    static void givenNewAttachment(Migration mig, String id) {
        def newAttachment = new AttachmentBuilder(id).build()
        when(mig.attachmentRepository.find(id)).thenReturn(null, newAttachment)
        when(mig.statusTrackingRepository.findLastEventRelevantToOutput(eq(id), any(), any())).thenReturn(null)
    }

    static void givenNewAttachmentMapping(Migration mig, String id) {
        when(mig.mappingRepository.getAttachmentMapping(id))
                .thenReturn(new MappingItem.Attachment(null, null, null, null, null, null))
    }
}
