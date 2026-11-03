package com.quadient.migration.example.common

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.example.Utils
import com.quadient.migration.shared.DataType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when

class GenerateSampleDataTest {
    @Test
    void resolvesReferencesNamesAndImplicitContainers() {
        def migration = mockVariables([
                clients: DataType.Array, address: DataType.SubTree, city: DataType.String,
                jobs   : DataType.Array, jobName: DataType.String, email: DataType.String, amount: DataType.Currency,
        ], [clients: "Clients", jobs: "jobs"])
        def structure = new VariableStructureBuilder("vs")
                .addVariable("clients", "Data")
                .addVariable("address", new VariableRef("clients"), "Address")
                .addVariable("city", new VariableRef("address"))
                .addVariable("jobs", new VariableRef("clients"), "Jobs")
                .addVariable("jobName", new VariableRef("jobs"))
                .addVariable("email", "Data.Clients.Value")
                .addVariable("amount", "Data.Clients.Value.Transactions.Value")
                .build()

        def fields = GenerateSampleData.resolveFields(migration, structure)

        def expected = """\
            Clients[]  Array  id=clients
              Address{}  SubTree  id=address
                city  String  id=city
              Jobs[]  Array  id=jobs
                jobName  String  id=jobName
              email  String  id=email
              Transactions[]  Array
                amount  Currency  id=amount""".stripIndent()
        Assertions.assertEquals(expected, GenerateSampleData.describe(fields.tree))
        Assertions.assertEquals([], fields.warnings)
    }

    @Test
    void generatesSeparateTopAndNestedArraySizesWithCyclingValues() {
        def migration = mockVariables([clients: DataType.Array, name: DataType.String, jobName: DataType.String, paid: DataType.Boolean])
        def structure = new VariableStructureBuilder("vs")
                .addVariable("clients", "Data", "Clients")
                .addVariable("name", new VariableRef("clients"))
                .addVariable("jobName", "Data.Clients.Value.Jobs.Value")
                .addVariable("paid", "Data.Clients.Value")
                .build()

        def fields = GenerateSampleData.resolveFields(migration, structure)
        def data = GenerateSampleData.generate(fields.tree, [name: ["John", "Maria"], jobName: ["a", "b", "c"]], 3, 2)

        def expected = [Clients: [
                [name: "John", Jobs: [[jobName: "a"], [jobName: "b"], [jobName: "c"]], paid: true],
                [name: "Maria", Jobs: [[jobName: "a"], [jobName: "b"], [jobName: "c"]], paid: true],
        ]]
        Assertions.assertEquals(expected, data)
    }

    @Test
    void reportsUnresolvableAndMissingVariables() {
        def migration = mockVariables([text: DataType.String, child: DataType.String])
        def structure = new VariableStructureBuilder("vs")
                .addVariable("text", "Data")
                .addVariable("child", new VariableRef("text"))
                .addVariable("orphan", "Data")
                .build()

        def fields = GenerateSampleData.resolveFields(migration, structure)

        Assertions.assertEquals("text  String  id=text\norphan  String  id=orphan", GenerateSampleData.describe(fields.tree))
        Assertions.assertEquals([
                "Variable 'text' of type String is used in a path, only Array and SubTree can be referenced",
                "Variable 'child' has no resolvable path - skipped",
                "Variable 'orphan' has no variable record - treated as String",
        ], fields.warnings)
    }

    @Test
    void selectsOnlyStructureOrFailsWithAvailableIds() {
        def migration = Utils.mockMigration()
        def only = new VariableStructureBuilder("only").build()

        when(migration.variableStructureRepository.listAll()).thenReturn([only])
        Assertions.assertSame(only, GenerateSampleData.selectStructure(migration, null))

        when(migration.variableStructureRepository.listAll()).thenReturn([])
        def none = Assertions.assertThrows(IllegalArgumentException) { GenerateSampleData.selectStructure(migration, null) }
        Assertions.assertEquals("Project 'testProject' has no variable structures.", none.message)

        when(migration.variableStructureRepository.listAll()).thenReturn([only, new VariableStructureBuilder("other").build()])
        def many = Assertions.assertThrows(IllegalArgumentException) { GenerateSampleData.selectStructure(migration, null) }
        Assertions.assertEquals("Project 'testProject' has 2 variable structures, specify one with --variable-structure-id: only, other", many.message)
    }

    @Test
    void defaultsAndJsonKeepDecimalsAndUnicode() {
        Assertions.assertEquals("string", GenerateSampleData.defaultValue(DataType.String))
        Assertions.assertEquals(0, GenerateSampleData.defaultValue(DataType.Integer))
        Assertions.assertEquals(true, GenerateSampleData.defaultValue(DataType.Boolean))

        def json = GenerateSampleData.toJson([name: "Müller", amount: new BigDecimal("200.00")])
        Assertions.assertEquals('{\n    "name": "Müller",\n    "amount": 200.00\n}', json.replaceAll("\\r\\n|\\r", "\n"))
    }

    private static Migration mockVariables(Map<String, DataType> types, Map<String, String> names = [:]) {
        def migration = Utils.mockMigration()
        when(migration.variableRepository.find(anyString())).thenReturn(null)
        types.each { id, type ->
            def variable = new VariableBuilder(id).dataType(type).name(names[id]).build()
            when(migration.variableRepository.find(id)).thenReturn(variable)
        }
        return migration
    }
}
