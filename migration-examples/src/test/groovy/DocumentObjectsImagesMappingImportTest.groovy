import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.example.common.mapping.DocumentObjectsImagesImport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.ImageType
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class DocumentObjectsImagesMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesDocumentObjectName() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            unchanged,,Block,false,[],,,,Active
            kept,keptName,Block,false,[],,,,Active
            overridden,someName,Block,false,[],,,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null,null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", "someName", false, null, null, null,null)
        givenExistingDocumentObjectMapping(migration, "kept", "keptName", null, null, null, null, null)
        givenExistingDocumentObject(migration, "overridden", "previousName", false, null, null, null,null)
        givenExistingDocumentObjectMapping(migration, "overridden", "previousName", null, null, null, null, null)

        DocumentObjectsImagesImport.runDocumentObjects(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject("keptName", null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject("someName", null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }

    @Test
    void overridesDocumentObjectInternal() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            unchanged,,Block,false,[],,,,Active
            kept,,Block,true,[],,,,Active
            overridden,,Block,true,[],,,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", null, true, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "kept", null, true, null, null, null, null)
        givenExistingDocumentObject(migration, "overridden", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "overridden", null, false, null, null, null, null)

        DocumentObjectsImagesImport.runDocumentObjects(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, true, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, true, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }

    @Test
    void overridesDocumentObjectBaseTemplate() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            unchanged,,Block,false,[],,,,Active
            kept,,Block,false,[],keptTemplate,,,Active
            overridden,,Block,false,[],overriddenTemplate,,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "kept", null, null, "keptTemplate", null, null, null)
        givenExistingDocumentObject(migration, "overridden", null, false, "previousTemplate", null, null, null)
        givenExistingDocumentObjectMapping(migration, "overridden", null, null, "previousTemplate", null, null, null)

        DocumentObjectsImagesImport.runDocumentObjects(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, "keptTemplate", null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, "overriddenTemplate", null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }

    @Test
    void overridesDocumentObjectTargetFolder() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            unchanged,,Block,false,[],,,,Active
            kept,,Block,false,[],,keptFolder,,Active
            overridden,,Block,false,[],,overriddenFolder,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "kept", null, null, null, "keptFolder", null, null)
        givenExistingDocumentObject(migration, "overridden", null, false, null, "previousFolder", null, null)
        givenExistingDocumentObjectMapping(migration, "overridden", null, null, null, "previousFolder", null, null)

        DocumentObjectsImagesImport.runDocumentObjects(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, null, "keptFolder", null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, null, "overriddenFolder", null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }

    @Test
    void overridesVariableStructureRef() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            unchanged,,Block,false,[],,,,Active
            kept,,Block,false,[],,,keptVarStructure,Active
            overridden,,Block,false,[],,,overriddenVarStructure,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", null, false, null, null, null, "keptVarStructure")
        givenExistingDocumentObjectMapping(migration, "kept", null, null, null, null, null, "keptVarStructure")
        givenExistingDocumentObject(migration, "overridden", null, false, null, null, null,"previousVarStructure")
        givenExistingDocumentObjectMapping(migration, "overridden", null, null, null,null,null,"overriddenVarStructure")

        DocumentObjectsImagesImport.runDocumentObjects(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null,null,null,null,null,null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null,null,null,null,null,"keptVarStructure"))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null,null,null,null,null,"overriddenVarStructure"))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }


    @Test
    void overridesDocumentObjectType() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            unchanged,,Block,false,[],,,,Active
            kept,,Template,false,[],,,,Active
            overridden,,Page,false,[],,,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "kept", null, null, null, null, DocumentObjectType.Template, null)
        givenExistingDocumentObject(migration, "overridden", null, false, null, null, DocumentObjectType.Template, null)
        givenExistingDocumentObjectMapping(migration, "overridden", null, null, null, null, DocumentObjectType.Section, null)

        DocumentObjectsImagesImport.runDocumentObjects(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, null, null, DocumentObjectType.Template, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, null, null, DocumentObjectType.Page, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }


    @Test
    void overridesImageName() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,sourcePath,originLocation,targetFolder,status
            unchanged,,,[],,Active
            kept,keptName,,[],,Active
            overridden,someName,,[],,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null)
        givenExistingImage(migration, "kept", "someName", null, null)
        givenExistingImageMapping(migration, "kept", "keptName", null, null)
        givenExistingImage(migration, "overridden", "otherName", null, null)
        givenExistingImageMapping(migration, "overridden", null, null, null)

        DocumentObjectsImagesImport.runImages(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image("keptName", null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image("someName", null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesImageTargetFolder() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,sourcePath,originLocation,targetFolder,status
            unchanged,,,[],,Active
            kept,,,[],keptFolder,Active
            overridden,,,[],overrideFolder,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null)
        givenExistingImage(migration, "kept", null, null, null)
        givenExistingImageMapping(migration, "kept", null, "keptFolder", null)
        givenExistingImage(migration, "overridden", null, "someFolder", null)
        givenExistingImageMapping(migration, "overridden", null, "overrideFolder", null)

        DocumentObjectsImagesImport.runImages(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, "keptFolder", null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, "overrideFolder", null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    @Test
    void overridesImageSourcePath() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,sourcePath,originLocation,targetFolder,status
            unchanged,,,[],,Active
            kept,,keptPath,[],,Active
            overridden,,overridePath,[],,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingImage(migration, "unchanged", null, null, null)
        givenExistingImageMapping(migration, "unchanged", null, null, null)
        givenExistingImage(migration, "kept", null, null, "keptPath")
        givenExistingImageMapping(migration, "kept", null, null, "keptPath")
        givenExistingImage(migration, "overridden", null, null, "somePath")
        givenExistingImageMapping(migration, "overridden", null, null, "overridePath")

        DocumentObjectsImagesImport.runImages(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Image(null, null, null))
        verify(migration.mappingRepository, times(1)).applyImageMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Image(null, null, "keptPath"))
        verify(migration.mappingRepository, times(1)).applyImageMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Image(null, null, "overridePath"))
        verify(migration.mappingRepository, times(1)).applyImageMapping("overridden")
    }

    static void givenExistingDocumentObject(Migration mig, String id, String name, Boolean internal, String baseTemplate, String targetFolder, DocumentObjectType type, String varStructureRef) {
        when(mig.documentObjectRepository.find(id)).thenReturn(new DocumentObject(id, name, [], new CustomFieldMap([:]), type ?: DocumentObjectType.Block, [], internal, targetFolder, null, varStructureRef ? new VariableStructureRef(varStructureRef) : null, baseTemplate, null, null, null))
    }

    static void givenExistingDocumentObjectMapping(
        Migration mig,
        String id,
        String name,
        Boolean internal,
        String baseTemplate,
        String targetFolder,
        DocumentObjectType type,
        String varStructureRef
    ) {
        when(mig.mappingRepository.getDocumentObjectMapping(id))
            .thenReturn(new MappingItem.DocumentObject(name, internal, baseTemplate, targetFolder, type, varStructureRef))
    }

    static void givenExistingImage(Migration mig, String id, String name, String targetFolder, String sourcePath) {
        when(mig.imageRepository.find(id)).thenReturn(new Image(id, name, [], new CustomFieldMap([:]), sourcePath, null, ImageType.Png, targetFolder))
    }

    static void givenExistingImageMapping(
        Migration mig,
        String id,
        String name,
        String targetFolder,
        String sourcePath
    ) {
        when(mig.mappingRepository.getImageMapping(id))
            .thenReturn(new MappingItem.Image(name, targetFolder, sourcePath))
    }
}
