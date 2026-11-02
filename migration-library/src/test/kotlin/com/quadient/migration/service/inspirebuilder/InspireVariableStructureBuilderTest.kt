package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.shared.DataType
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.internal.layoutnodes.data.LAVariableIface.VariableType
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tools.jackson.databind.JsonNode
import tools.jackson.dataformat.xml.XmlMapper

class InspireVariableStructureBuilderTest {
    private val variables = mockk<VariableRepository>()
    private val structures = mockk<VariableStructureRepository>()
    private val subject = InspireVariableStructureBuilder(variables, structures, aProjectConfig())
    private val xmlMapper = XmlMapper()

    @Test
    fun `data definition imports leaves and array paths together regardless of declaration order`() {
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
        assertDataVariable(nodes, "CustomerName", "Value", "String")
        assertDataVariable(nodes, "Clients", null, "Array")
        assertDataVariable(nodes, "Value", "Clients", "SubTree", -1)
        assertDataVariable(nodes, "Count", "Clients", "Int", -2)
    }

    @Test
    fun `data definition imports leaves under subtrees`() {
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
        assertDataVariable(nodes, "Customer", null, "SubTree")
        assertDataVariable(nodes, "Number", "Customer", "Int")
    }

    @ParameterizedTest
    @EnumSource(DataType::class)
    fun `data definition imports each model type as an input variable even without children`(type: DataType) {
        val variable = VariableBuilder("variable").name("Variable").dataType(type).build()
        every { variables.findOrFail(variable.id) } returns variable
        val structure = VariableStructureBuilder("structure").addVariable(variable.id, "Data").build()

        val xml = xmlMapper.readTree(subject.buildDataDefinition(structure))
        val nodes = xml["Variable"].toList()

        assertDataVariable(nodes, "Variable", null, type.toInteractiveDataType())
        nodes.any { it["Type"]?.stringValue() == "Disconnected" }.shouldBeEqualTo(false)
        if (type == DataType.Array) {
            assertDataVariable(nodes, "Value", "Variable", "SubTree", -1)
            assertDataVariable(nodes, "Count", "Variable", "Int", -2)
        }
    }

    @Test
    fun `data definition supports nested arrays aliases and implicit parent paths`() {
        val clients = VariableBuilder("clients").name("OriginalClients").dataType(DataType.Array).build()
        val jobs = VariableBuilder("jobs").name("OriginalJobs").dataType(DataType.Array).build()
        val salary = VariableBuilder("salary").name("Salary").dataType(DataType.Currency).build()
        val city = VariableBuilder("city").name("City").dataType(DataType.String).build()
        listOf(clients, jobs, salary, city).forEach { every { variables.findOrFail(it.id) } returns it }
        val structure = VariableStructureBuilder("structure")
            .addVariable(salary.id, jobs, "Pay")
            .addVariable(city.id, "Data.Customers.Value.Address")
            .addVariable(jobs.id, clients, "Positions")
            .addVariable(clients.id, "Data", "Customers")
            .build()

        val xml = xmlMapper.readTree(subject.buildDataDefinition(structure))
        val nodes = xml["Variable"].toList()

        assertDataVariable(nodes, "Customers", null, "Array")
        val positions = assertDataVariable(nodes, "Positions", "Value", "Array")
        val positionValue = nodes.single { it["Name"]?.stringValue() == "Value" && it["ParentId"] == positions["Id"] }
        val pay = nodes.single { it["Name"]?.stringValue() == "Pay" }
        pay["ParentId"].shouldBeEqualTo(positionValue["Id"])
        val payProperties = nodes.single { it["Id"] == pay["Id"] && it.has("Type") }
        payProperties["Type"].stringValue().shouldBeEqualTo("DataVariable")
        payProperties["VarType"].stringValue().shouldBeEqualTo("Currency")
        assertDataVariable(nodes, "Address", "Value", "SubTree")
        assertDataVariable(nodes, "City", "Address", "String")
    }

    @Test
    fun `data definition keeps built in system variables without duplicating their parent`() {
        val pageCounter = VariableBuilder("page").name("PageCounter").dataType(DataType.Integer).build()
        every { variables.findOrFail(pageCounter.id) } returns pageCounter
        val structure = VariableStructureBuilder("structure")
            .addVariable(pageCounter.id, "Data.SystemVariable")
            .build()

        val nodes = xmlMapper.readTree(subject.buildDataDefinition(structure))["Variable"].toList()

        nodes.count { it["Id"]?.stringValue() == "Def.SystemVariables" }.shouldBeEqualTo(1)
        assertDataVariable(nodes, "PageCounter", null, "Int", -11)
    }

    @Test
    fun `document initialization still imports only paths and leaves used variables disconnected`() {
        val name = VariableBuilder("name").name("Name").dataType(DataType.String).defaultValue("Alice").build()
        val structure = VariableStructureBuilder("structure")
            .addVariable(name.id, "Data.Clients.Value")
            .build()
        every { structures.findOrFail(structure.id) } returns structure
        val layout = WfdXmlBuilder().addLayout()

        subject.initVariableStructure(layout, structure.id).shouldBeEqualTo(structure)

        (layout.data.findVariable("Clients") as VariableImpl).type.shouldBeEqualTo(VariableType.DATA_VARIABLE)
        assertThrows<IllegalArgumentException> { layout.data.findVariable("Clients", "Name") }
        val leaf = getOrCreateVariable(layout.data, name.nameOrId(), name, "Data.Clients.Value")
        (leaf as VariableImpl).type.shouldBeEqualTo(VariableType.DISCONNECTED)
        leaf.constantText.shouldBeEqualTo("Alice")
    }

    @Test
    fun `data definition rejects leaf variables used as parents`() {
        val parent = VariableBuilder("parent").name("Parent").dataType(DataType.String).build()
        val child = VariableBuilder("child").name("Child").dataType(DataType.String).build()
        listOf(parent, child).forEach { every { variables.findOrFail(it.id) } returns it }
        val structure = VariableStructureBuilder("structure")
            .addVariable(parent.id, "Data")
            .addVariable(child.id, "Data.Parent")
            .build()

        assertThrows<IllegalArgumentException> { subject.buildDataDefinition(structure) }
    }

    @Test
    fun `empty data definition still exports system variables`() {
        val structure = VariableStructureBuilder("empty").build()
        every { structures.findOrFail(structure.id) } returns structure

        val nodes = xmlMapper.readTree(subject.buildDataDefinition(structure))["Variable"].toList()

        nodes.count { it["Name"]?.stringValue() == "PageCounter" }.shouldBeEqualTo(1)
        nodes.count { it["Id"]?.stringValue() == "Def.SystemVariables" }.shouldBeEqualTo(1)
    }

    private fun assertDataVariable(
        nodes: List<JsonNode>, name: String, parentName: String?, varType: String, insideFnc: Int = 0,
    ): JsonNode {
        val declaration = nodes.single { it["Name"]?.stringValue() == name }
        if (parentName != null) {
            nodes.any { it["Name"]?.stringValue() == parentName && it["Id"] == declaration["ParentId"] }
                .shouldBeEqualTo(true)
        }
        val properties = nodes.single { it["Id"] == declaration["Id"] && it.has("Type") }
        properties["Type"].stringValue().shouldBeEqualTo("DataVariable")
        properties["VarType"].stringValue().shouldBeEqualTo(varType)
        properties["InsideFnc"].stringValue().shouldBeEqualTo(insideFnc.toString())
        properties.has("Content").shouldBeEqualTo(false)
        return declaration
    }
}
