import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.example.common.mapping.VariablesExport
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.VariablePathData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class VariablesMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossible() {
        Path mappingFile = Paths.get(dir.path, "testProject-variable-structure-test.csv")
        def migration = Utils.mockMigration()

        when(migration.variableRepository.listAll()).thenReturn([
                new Variable("empty", null, [], new CustomFieldMap([:]), DataType.String, null),
                new Variable("full", "full name", ["foo", "bar"], new CustomFieldMap([foo: "bar", bar: "baz"]), DataType.Boolean, "default"),
                new Variable("overridden", "full name", ["foo", "bar"], new CustomFieldMap([foo: "bar", bar: "baz"]), DataType.Boolean, "default"),
        ])
        when(migration.mappingRepository.getVariableMapping(any())).thenReturn(new MappingItem.Variable(null, null))
        when(migration.mappingRepository.getVariableMapping("overridden")).thenReturn(new MappingItem.Variable(null, DataType.Double))
        when(migration.mappingRepository.getVariableStructureMapping(any())).thenReturn(new MappingItem.VariableStructure("", ["overridden": new VariablePathData("override/path", "overridden name")]))

        VariablesExport.run(migration, mappingFile)

        def text = mappingFile.toFile().text

        def expectedResult =
                """id,name,origin_locations,inspire_path,data_type
empty,,[],,String
full,,[foo; bar],,Boolean
overridden,,[foo; bar],,Boolean
"""

        Assertions.assertEquals(expectedResult, text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void emptyFileWhenNoVariables() {
        Path mappingFile = Paths.get(dir.path, "testProject-variable-structure-test.csv")
        def migration = Utils.mockMigration()

        when(migration.variableRepository.listAll()).thenReturn([])
        when(migration.mappingRepository.getVariableMapping(any())).thenReturn(new MappingItem.Variable(null, null))

        VariablesExport.run(migration, mappingFile)

        def text = mappingFile.toFile().text

        def expectedResult = "id,name,origin_locations,inspire_path,data_type\n"

        Assertions.assertEquals(expectedResult, text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
