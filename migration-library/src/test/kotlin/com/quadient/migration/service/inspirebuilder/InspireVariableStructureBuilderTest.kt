package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.shared.DataType
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.xml.XmlMapper

class InspireVariableStructureBuilderTest {
    private val variables = mockk<VariableRepository>()
    private val structures = mockk<VariableStructureRepository>()
    private val subject = InspireVariableStructureBuilder(variables, structures, aProjectConfig())
    private val xmlMapper = XmlMapper()

    @Test
    fun `data definition initializes array paths before declaring listed variables`() {
        val array = VariableBuilder("clients").name("Clients").dataType(DataType.Array).build()
        val name = VariableBuilder("name").name("OriginalName").dataType(DataType.String).defaultValue("Alice").build()
        every { variables.findOrFail(array.id) } returns array
        every { variables.findOrFail(name.id) } returns name
        val structure = VariableStructureBuilder("structure")
            .addVariable(name.id, array, "CustomerName")
            .addVariable(array.id, "Data")
            .build()
        every { structures.findOrFail(structure.id) } returns structure

        val nodes = xmlMapper.readTree(subject.buildDataDefinition(structure))["Variable"].toList()

        nodes.count { it["Name"]?.stringValue() == "Clients" }.shouldBeEqualTo(1)
        nodes.count { it["Name"]?.stringValue() == "Value" }.shouldBeEqualTo(1)
        val customerName = nodes.first { it["Name"]?.stringValue() == "CustomerName" }
        customerName["ParentId"].stringValue().shouldBeEqualTo("Data.Clients.Value")
        nodes.first { it["Id"] == customerName["Id"] && it.has("Content") }["Content"].stringValue()
            .shouldBeEqualTo("Alice")
    }

    @Test
    fun `data definition initializes subtree paths before declaring listed variables`() {
        val subtree = VariableBuilder("customer").name("Customer").dataType(DataType.SubTree).build()
        val number = VariableBuilder("number").name("Number").dataType(DataType.Integer).defaultValue("42").build()
        every { variables.findOrFail(subtree.id) } returns subtree
        every { variables.findOrFail(number.id) } returns number
        val structure = VariableStructureBuilder("structure")
            .addVariable(subtree.id, "Data")
            .addVariable(number.id, subtree)
            .build()
        every { structures.findOrFail(structure.id) } returns structure

        val nodes = xmlMapper.readTree(subject.buildDataDefinition(structure))["Variable"].toList()

        nodes.count { it["Name"]?.stringValue() == "Customer" }.shouldBeEqualTo(1)
        nodes.first { it["Name"]?.stringValue() == "Number" }["ParentId"].stringValue()
            .shouldBeEqualTo("Data.Customer")
        nodes.count { it["Content"]?.stringValue() == "42" }.shouldBeEqualTo(1)
    }

    @Test
    fun `empty data definition still exports system variables`() {
        val structure = VariableStructureBuilder("empty").build()
        every { structures.findOrFail(structure.id) } returns structure

        val nodes = xmlMapper.readTree(subject.buildDataDefinition(structure))["Variable"].toList()

        nodes.count { it["Name"]?.stringValue() == "PageCounter" }.shouldBeEqualTo(1)
        nodes.count { it["Id"]?.stringValue() == "Def.SystemVariables" }.shouldBeEqualTo(1)
    }
}
