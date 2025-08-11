import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.example.common.mapping.VariablesImport
import com.quadient.migration.shared.DataType
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
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,origin_locations,inspire_path,data_type
            unchangedEmpty,,[],,String
            unchangedPath,,[],oldPath,String
            withPath,,[],newPath,String
            withPathEmpty,,[],newPath,String
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingVariable(migration, "unchangedEmpty", null, DataType.String, null)
        givenExistingMapping(migration, "unchangedEmpty", null, null, null)
        givenExistingVariable(migration, "unchangedPath", null, DataType.String, "oldPath")
        givenExistingMapping(migration, "unchangedPath", null, null, "oldPath")
        givenExistingVariable(migration, "withPath", null, DataType.String, "existingPath")
        givenExistingMapping(migration, "withPath", null, null, "existingPath")
        givenExistingVariable(migration, "withPathEmpty", null, DataType.String, null)
        givenExistingMapping(migration, "withPathEmpty", null, null, "existingPath")

        VariablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchangedEmpty", new MappingItem.Variable(null, null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchangedEmpty", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("unchangedPath", new MappingItem.Variable(null, null, "oldPath"))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchangedPath", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("withPath", new MappingItem.Variable(null, null, "newPath"))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("withPath", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("withPathEmpty", new MappingItem.Variable(null, null, "newPath"))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("withPathEmpty", "testProject-variables")
    }

    @Test
    void overridesDataType() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,origin_locations,inspire_path,data_type
            unchanged,,[],,String
            kept,,[],,String
            overridden,,[],,Boolean
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingVariable(migration, "unchanged", null, DataType.String, null)
        givenExistingMapping(migration, "unchanged", null, null, null)
        givenExistingVariable(migration, "kept", null, DataType.String, null)
        givenExistingMapping(migration, "kept", null, DataType.String, null)
        givenExistingVariable(migration, "overridden", null, DataType.String, null)
        givenExistingMapping(migration, "overridden", null, DataType.Currency, null)

        VariablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Variable(null, null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchanged", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Variable(null, DataType.String, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("kept", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Variable(null, DataType.Boolean, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("overridden", "testProject-variables")
    }

    @Test
    void overridesName() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,origin_locations,inspire_path,data_type
            unchanged,,[],,String
            kept,someName,[],,String
            overridden,someName,[],,String
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingVariable(migration, "unchanged", null, null, null)
        givenExistingMapping(migration, "unchanged", null, null, null)
        givenExistingVariable(migration, "kept", "someName", null, null)
        givenExistingMapping(migration, "kept", "someName", null, null)
        givenExistingVariable(migration, "overridden", null, null, null)
        givenExistingMapping(migration, "overridden", null, null, null)

        VariablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.Variable(null, null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("unchanged", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.Variable("someName", null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("kept", "testProject-variables")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.Variable("someName", null, null))
        verify(migration.mappingRepository, times(1)).applyVariableMapping("overridden", "testProject-variables")
    }

    static void givenExistingVariable(Migration mig, String id, String name, DataType dataType, String inspirePath = null) {
        when(mig.variableRepository.find(id)).thenReturn(
                new Variable(id, name, [], new CustomFieldMap([:]), dataType ?: DataType.String, inspirePath)
        )
    }

    static void givenExistingMapping(
            Migration mig,
            String id,
            String name = null,
            DataType dataType = DataType.String,
            String inspirePath = null
    ) {
        when(mig.mappingRepository.getVariableMapping(id)).thenReturn(new MappingItem.Variable(name, dataType, inspirePath))
    }
}

