import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.DisplayRulesExport
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class DisplayRulesMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleFields() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.displayRuleRepository.listAll()).thenReturn([
                new DisplayRule("empty", null, [], new CustomFieldMap([:]), null, null, null, null, true, [:], null, null, null, null),
                new DisplayRule("full", "full", ["foo", "bar"], new CustomFieldMap([:]), null, null, null, "targetId1", false, [:], null, "targetFolder1", "baseTemplate1", new VariableStructureRef("varStruct1")),
                new DisplayRule("with-variable-structure", "with-var-struct", [], new CustomFieldMap([:]), null, null, null, null, true, [:], null, null, null, new VariableStructureRef("varStruct2")),
                new DisplayRule("overridden empty", null, [], new CustomFieldMap([:]), null, null, null, null, true, [:], null, null, null, null),
                new DisplayRule("overridden full", "full", ["foo", "bar"], new CustomFieldMap(["originalName": "originalFull"]), null, null, null, "targetId2", false, [:], null, "targetFolder2", "baseTemplate2", null),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        DisplayRulesExport.run(migration, mappingFile.toFile())

        def expected = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            empty,,true,,,,,Active,,[]
            full,full,false,baseTemplate1,targetFolder1,targetId1,varStruct1,Active,,[foo; bar]
            with-variable-structure,with-var-struct,true,,,,varStruct2,Active,,[]
            overridden empty,,true,,,,,Active,,[]
            overridden full,full,false,baseTemplate2,targetFolder2,targetId2,,Active,originalFull,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
