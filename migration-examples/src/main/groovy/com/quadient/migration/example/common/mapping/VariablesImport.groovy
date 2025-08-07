package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.shared.DataType
import com.quadient.migration.example.common.util.Csv

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def variablesMappingDir = Paths.get("mapping", "variables").toFile()
def csvFiles = variablesMappingDir.listFiles()?.findAll { it.name.toLowerCase().endsWith(".csv") } ?: []

if (csvFiles.isEmpty()) {
    println "No CSV files found in mapping/variables/. Cannot import variable structure."
    System.exit(1)
}

println "Available CSV files for import:"
csvFiles.eachWithIndex { file, i -> println "${i + 1} - ${file.name}" }
println "Select a number of the CSV file to import:"

def selectedFile
while (true) {
    def userInput = System.in.newReader().readLine().trim()
    if (userInput.isInteger()) {
        def idx = userInput.toInteger() - 1
        if (idx >= 0 && idx < csvFiles.size()) {
            selectedFile = csvFiles[idx]
            println "Selected file: ${selectedFile.name}"
            break
        }
    }
    println "Invalid selection. Please enter a valid number:"
}

def lines = selectedFile.readLines()
def columnNames = lines.removeFirst().split(",")

def builder = new VariableStructureBuilder(selectedFile.name.split("\\.")[0]  )

for (line in lines) {
    def values = Csv.getCells(line, columnNames)
    builder.structure.put(values.get("id"), values.get("inspire_path"))

    def variable = migration.variableRepository.find(values.get("id"))
    variable.name = values.get("name")
    variable.dataType = Csv.deserialize(values.get("data_type"), DataType.class)
    migration.variableRepository.upsert(variable)
}

def structure = builder.build()
for (key in structure.structure.keySet().toList()) {
    if (structure.structure.get(key) == "") {
        structure.structure.remove(key)
    }
}
migration.variableStructureRepository.upsert(structure)