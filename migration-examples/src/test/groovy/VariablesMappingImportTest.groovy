import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.example.common.mapping.VariablesImport
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.VariablePathData
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class VariablesMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesInspirePath() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variable-structure-test.csv")
        def input = """\
            id,name,origin_locations,inspire_path,data_type
            unchangedEmpty,,[],,String
            unchangedPath,,[],oldPath,String
            withPath,,[],newPath,String
            withPathEmpty,,[],newPath,String
            """.stripIndent()
        mappingFile.toFile().write(input)

        Map<String, VariablePathData> mappings = [:]
        givenExistingVariable(migration, "unchangedEmpty", null, DataType.String, null)
        givenExistingMapping(migration, "unchangedEmpty", null, null, null, mappings)
        givenExistingVariable(migration, "unchangedPath", null, DataType.String, "oldPath")
        givenExistingMapping(migration, "unchangedPath", null, null, "oldPath", mappings)
        givenExistingVariable(migration, "withPath", null, DataType.String, "existingPath")
        givenExistingMapping(migration, "withPath", null, null, "existingPath", mappings)
        givenExistingVariable(migration, "withPathEmpty", null, DataType.String, null)
        givenExistingMapping(migration, "withPathEmpty", null, null, "existingPath", mappings)

        VariablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchangedEmpty", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchangedEmpty")
        verify(migration.mappingRepository, times(1)).upsert("unchangedPath", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchangedPath")
        verify(migration.mappingRepository, times(1)).upsert("withPath", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("withPath")
        verify(migration.mappingRepository, times(1)).upsert("withPathEmpty", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("withPathEmpty")
        verify(migration.mappingRepository, times(1)).upsert("test", new MappingItem.VariableStructure(null, ["unchangedPath": new VariablePathData("oldPath", null),
                                                                                                              "withPath"     : new VariablePathData("newPath", null),
                                                                                                              "withPathEmpty": new VariablePathData("newPath", null),]))
        verify(migration.mappingRepository, times(1)).applyVariableStructureMapping("test")
    }

    @Test
    void overridesDataType() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variable-structure-test.csv")
        def input = """\
            id,name,origin_locations,inspire_path,data_type
            unchanged,,[],,String
            kept,,[],,String
            overridden,,[],,Boolean
            """.stripIndent()
        mappingFile.toFile().write(input)

        Map<String, VariablePathData> mappings = [:]
        givenExistingVariable(migration, "unchanged", null, DataType.String, null)
        givenExistingMapping(migration, "unchanged", null, null, null, mappings)
        givenExistingVariable(migration, "kept", null, DataType.String, null)
        givenExistingMapping(migration, "kept", null, DataType.String, null, mappings)
        givenExistingVariable(migration, "overridden", null, DataType.String, null)
        givenExistingMapping(migration, "overridden", null, DataType.Currency, null, mappings)

        VariablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Variable(null, DataType.String))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Variable(null, DataType.Boolean))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("overridden")
    }

    @Test
    void overridesName() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variable-structure-test.csv")
        def input = """\
            id,name,origin_locations,inspire_path,data_type
            unchanged,,[],,String
            kept,someName,[],,String
            overridden,someName,[],,String
            """.stripIndent()
        mappingFile.toFile().write(input)

        Map<String, VariablePathData> mappings = [:]
        givenExistingVariable(migration, "unchanged", null, null, null)
        givenExistingMapping(migration, "unchanged", null, null, null, mappings)
        givenExistingVariable(migration, "kept", "someName", null, null)
        givenExistingMapping(migration, "kept", "someName", null, null, mappings)
        givenExistingVariable(migration, "overridden", null, null, null)
        givenExistingMapping(migration, "overridden", null, null, null, mappings)

        VariablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Variable(null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("overridden")
        verify(migration.mappingRepository, times(1)).upsert("test", new MappingItem.VariableStructure(null, ["kept"      : new VariablePathData("", "someName"),
                                                                                                              "overridden": new VariablePathData("", "someName"),]))
    }

    static void givenExistingVariable(Migration mig, String id, String name, DataType dataType, String inspirePath = null) {
        when(mig.variableRepository.find(id)).thenReturn(new Variable(id, name, [], new CustomFieldMap([:]), dataType ?: DataType.String, inspirePath))
    }

    static void givenExistingMapping(Migration mig,
                                     String variableId,
                                     String name = null,
                                     DataType dataType = DataType.String,
                                     String inspirePath = null,
                                     Map<String, VariablePathData> mappings) {
        if (!mappings.containsKey(variableId) && inspirePath != null && inspirePath != "") {
            mappings[variableId] = new VariablePathData(inspirePath, name)
        }

        when(mig.mappingRepository.getVariableMapping(variableId)).thenReturn(new MappingItem.Variable(null, dataType))
        when(mig.mappingRepository.getVariableStructureMapping(any())).thenReturn(new MappingItem.VariableStructure(null, mappings))
    }
}

