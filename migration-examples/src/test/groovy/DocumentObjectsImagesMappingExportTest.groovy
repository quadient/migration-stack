import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.DocumentObjectsImagesExport
import com.quadient.migration.shared.DocumentObjectType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class DocumentObjectsImagesMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleDocumentObjects() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.documentObjectRepository.listAll()).thenReturn([
            new DocumentObject("empty", null, [], new CustomFieldMap([:]), DocumentObjectType.Block, [], false, null, null, null, null, null, null, null),
            new DocumentObject("should not be listed because internal", null, [], new CustomFieldMap([:]), DocumentObjectType.Block, [], true, null, null, null, null, null, null, null),
            new DocumentObject("full", "full", ["foo", "bar"], new CustomFieldMap([:]), DocumentObjectType.Page, [], false, "someDir", null, new VariableStructureRef("struct"), "tmpl.wfd", null, null, null),
            new DocumentObject("overridden empty", null, [], new CustomFieldMap([:]), DocumentObjectType.Block, [], false, null, null, null, null, null, null, null),
            new DocumentObject("overridden full", "full", ["foo", "bar"], new CustomFieldMap([:]), DocumentObjectType.Page, [], false, "someDir", null, new VariableStructureRef("struct"),"tmpl.wfd", null, null, null),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())
        when(migration.mappingRepository.getDocumentObjectMapping(any()))
                .thenReturn(new MappingItem.DocumentObject(null, null, null, null, null, null))
        when(migration.mappingRepository.getDocumentObjectMapping("overridden empty"))
                .thenReturn(new MappingItem.DocumentObject("newName", true, "tmpl.wfd", "someDir", DocumentObjectType.Section, "new struct"))
        when(migration.mappingRepository.getDocumentObjectMapping("overridden full"))
                .thenReturn(new MappingItem.DocumentObject("newName", true, "tmpl.wfd", "someDir", DocumentObjectType.Section, "new struct"))

        DocumentObjectsImagesExport.run(migration, mappingFile, Paths.get(dir.path, "images"))

        def expected = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            empty,,Block,false,[],,,,Active
            full,full,Page,false,[foo; bar],tmpl.wfd,someDir,struct,Active
            overridden empty,newName,Section,true,[],tmpl.wfd,someDir,new struct,Active
            overridden full,newName,Section,true,[foo; bar],tmpl.wfd,someDir,new struct,Active
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text)
    }

    @Test
    void allPossibleImages() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        "id,name,sourcePath,originLocation,targetFolder,status"
        when(migration.imageRepository.listAll()).thenReturn([
            new Image("empty", null, [], new CustomFieldMap([:]), null, null, null, null),
            new Image("full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", null, null, "targetDir"),
            new Image("overridden empty", null, [], new CustomFieldMap([:]), null, null, null, null),
            new Image("overridden full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", null, null, "targetDir"),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())
        when(migration.mappingRepository.getImageMapping(any()))
                .thenReturn(new MappingItem.Image(null, null, null))
        when(migration.mappingRepository.getImageMapping("overridden empty"))
                .thenReturn(new MappingItem.Image("newName", "newTargetDir", "newSourcePath"))
        when(migration.mappingRepository.getImageMapping("overridden full"))
                .thenReturn(new MappingItem.Image("newName", "newTargetDir", "newSourcePath"))

        DocumentObjectsImagesExport.run(migration, Paths.get(dir.path, "docObjs"), mappingFile)

        def expected = """\
            id,name,sourcePath,originLocation,targetFolder,status
            empty,,,[],,Active
            full,full,sourcePath,[foo; bar],targetDir,Active
            overridden empty,newName,newSourcePath,[],newTargetDir,Active
            overridden full,newName,newSourcePath,[foo; bar],newTargetDir,Active
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text)
    }
}
