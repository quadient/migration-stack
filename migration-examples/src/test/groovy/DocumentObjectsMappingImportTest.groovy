import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.example.common.mapping.DocumentObjectsImport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class DocumentObjectsMappingImportTest {
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
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", "someName", false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "kept", "keptName", null, null, null, null, null)
        givenExistingDocumentObject(migration, "overridden", "previousName", false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "overridden", "previousName", null, null, null, null, null)

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject("keptName", null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject("someName", null, null, null, null, null, null))
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

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, true, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, true, null, null, null, null, null))
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

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, "keptTemplate", null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, "overriddenTemplate", null, null, null, null))
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

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, null, "keptFolder", null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, null, "overriddenFolder", null, null, null))
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
        givenExistingDocumentObject(migration, "overridden", null, false, null, null, null, "previousVarStructure")
        givenExistingDocumentObjectMapping(migration, "overridden", null, null, null, null, null, "overriddenVarStructure")

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, null, null, null, "keptVarStructure", null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, null, null, null, "overriddenVarStructure", null))
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

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, null, null, DocumentObjectType.Template, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, null, null, DocumentObjectType.Page, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }

    @Test
    void overridesSkipOptions() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status,skip,skipPlaceholder,skipReason
            unchanged,,Block,false,[],,,,Active
            kept,,Template,false,[],,,,Active
            overridden,,Page,false,[],,,,Active,true,placeholder,reason
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "kept", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "kept", null, null, null, null, DocumentObjectType.Template, null)
        givenExistingDocumentObject(migration, "overridden", null, false, null, null, DocumentObjectType.Template, null)
        givenExistingDocumentObjectMapping(migration, "overridden", null, null, null, null, DocumentObjectType.Section, null)

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DocumentObject(null, null, null, null, null, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DocumentObject(null, null, null, null, DocumentObjectType.Template, null, null))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DocumentObject(null, null, null, null, DocumentObjectType.Page, null, new SkipOptions(true, "placeholder", "reason")))
        verify(migration.mappingRepository, times(1)).applyDocumentObjectMapping("overridden")
    }

    static void givenExistingDocumentObject(Migration mig, String id, String name, Boolean internal, String baseTemplate, String targetFolder, DocumentObjectType type, String varStructureRef) {
        when(mig.documentObjectRepository.find(id)).thenReturn(new DocumentObject(id, name, [], new CustomFieldMap([:]), type ?: DocumentObjectType.Block, [], internal, targetFolder, null, varStructureRef ? new VariableStructureRef(varStructureRef) : null, baseTemplate, null, null, null, [:], new SkipOptions(false, null, null)))
    }

    static void givenExistingDocumentObjectMapping(Migration mig,
                                                   String id,
                                                   String name,
                                                   Boolean internal,
                                                   String baseTemplate,
                                                   String targetFolder,
                                                   DocumentObjectType type,
                                                   String varStructureRef) {
        when(mig.mappingRepository.getDocumentObjectMapping(id))
                .thenReturn(new MappingItem.DocumentObject(name, internal, baseTemplate, targetFolder, type, varStructureRef, null))
    }
}