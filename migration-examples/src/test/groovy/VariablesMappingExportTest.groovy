import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
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
        when(migration.variableStructureRepository.find("test")).thenReturn(new VariableStructure("struct", "", [], new CustomFieldMap([:]), ["overridden": new VariablePathData("override/path", "overridden name")], new VariableRef("full")))
        when(migration.mappingRepository.getVariableStructureMapping(any())).thenReturn(new MappingItem.VariableStructure("", ["overridden": new VariablePathData("override/path", "overridden name")], new VariableRef("full")))

        VariablesExport.run(migration, mappingFile)

        def text = mappingFile.toFile().text

        def expectedResult =
                """id,name,data_type,inspire_path,inspire_name,origin_locations,language_variable
empty,,String,,,[],,
full,full name,Boolean,,,[foo; bar],true,
overridden,full name,Boolean,override/path,overridden name,[foo; bar],,
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

        def expectedResult = "id,name,data_type,inspire_path,inspire_name,origin_locations,language_variable\n"

        Assertions.assertEquals(expectedResult, text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
