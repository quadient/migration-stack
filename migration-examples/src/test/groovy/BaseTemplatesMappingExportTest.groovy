import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.BaseTemplatesExport
import com.quadient.migration.shared.DocumentObjectType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class BaseTemplatesMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleFields() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.baseTemplateRepository.listAll()).thenReturn([
            new BaseTemplate("empty", null, [], new CustomFieldMap([:]), null, [], null),
            new BaseTemplate("full", "full", ["foo", "bar"], new CustomFieldMap([:]), "targetFolder1", [], new VariableStructureRef("varStruct1")),
            new BaseTemplate("overridden full", "full", ["foo", "bar"], new CustomFieldMap(["originalName": "originalFull"]), "targetFolder2", [], new VariableStructureRef("varStruct2")),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        BaseTemplatesExport.run(migration, mappingFile.toFile())

        def expected = """\
            id,name,targetFolder,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            empty,,,,Active,,[]
            full,full,targetFolder1,varStruct1,Active,,[foo; bar]
            overridden full,full,targetFolder2,varStruct2,Active,originalFull,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportsOnlySelectedBaseTemplates() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        def referencedBaseTemplate = new BaseTemplate("referenced", null, [], new CustomFieldMap([:]), null, [], null)
        def unselectedBaseTemplate = new BaseTemplate("unselected", null, [], new CustomFieldMap([:]), null, [], null)
        def selectedDocObject = new DocumentObjectBuilder("doc1", DocumentObjectType.Block).build()

        when(migration.projectConfig.getDocumentObjectsToProcess()).thenReturn(["doc1"])
        when(migration.documentObjectRepository.listIds(["doc1"])).thenReturn([selectedDocObject])
        when(migration.referenceCollector.collectAllObjects(selectedDocObject)).thenReturn([referencedBaseTemplate] as Set)
        when(migration.baseTemplateRepository.listAll()).thenReturn([referencedBaseTemplate, unselectedBaseTemplate])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        BaseTemplatesExport.run(migration, mappingFile.toFile())

        def expected = """\
            id,name,targetFolder,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            referenced,,,,Active,,[]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
