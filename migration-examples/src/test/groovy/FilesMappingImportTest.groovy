import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.FilesImport
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.*

class FilesMappingImportTest {
    @TempDir
    java.io.File dir

    @Test
    void importsFileMapping() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,sourcePath,fileType,targetFolder,status,originLocations,skip,skipPlaceholder,skipReason
            file1,newName,newPath,Document,newFolder,Active,[],false,,
            file2,,,Attachment,,Active,[],true,placeholder,reason
            """.stripIndent()
        mappingFile.toFile().write(input)
        
        givenExistingFile(migration, "file1", "oldName", "oldFolder", "oldPath", FileType.Attachment)
        givenExistingFileMapping(migration, "file1", "oldName", "oldFolder", "oldPath", FileType.Attachment)
        givenExistingFile(migration, "file2", "someName", "someFolder", "somePath", FileType.Document)
        givenExistingFileMapping(migration, "file2", null, null, null, null)

        FilesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("file1", new MappingItem.File("newName", "newFolder", "newPath", FileType.Document, new SkipOptions(false, null, null)))
        verify(migration.mappingRepository, times(1)).applyFileMapping("file1")
        verify(migration.mappingRepository, times(1)).upsert("file2", new MappingItem.File(null, null, null, FileType.Attachment, new SkipOptions(true, "placeholder", "reason")))
        verify(migration.mappingRepository, times(1)).applyFileMapping("file2")
    }

    static void givenExistingFile(Migration mig, String id, String name, String targetFolder, String sourcePath, FileType fileType) {
        when(mig.fileRepository.find(id)).thenReturn(new File(id, name, [], new CustomFieldMap([:]), sourcePath, targetFolder, fileType, new SkipOptions(false, null, null)))
    }

    static void givenExistingFileMapping(Migration mig,
                                          String id,
                                          String name,
                                          String targetFolder,
                                          String sourcePath,
                                          FileType fileType) {
        when(mig.mappingRepository.getFileMapping(id))
                .thenReturn(new MappingItem.File(name, targetFolder, sourcePath, fileType, null))
    }
}
