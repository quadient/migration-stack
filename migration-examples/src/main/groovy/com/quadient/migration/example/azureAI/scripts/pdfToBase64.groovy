package com.quadient.migration.example.azureAI.scripts

import com.quadient.migration.api.Migration

import static com.quadient.migration.example.common.util.InitMigration.initMigration

Migration migration = initMigration(this.binding)
def inputFolder = new File(migration.projectConfig.inputDataPath)
def outputFolder = new File(migration.projectConfig.inputDataPath)

if (!outputFolder.exists()) {
    outputFolder.mkdirs()
}

inputFolder.eachFileMatch(~/.*\.pdf/) { File pdfFile ->
    byte[] fileBytes = pdfFile.bytes
    String base64 = Base64.encoder.encodeToString(fileBytes)

    def outputFile = new File(outputFolder, pdfFile.name.replaceAll(/\.pdf$/, ".txt"))
    outputFile.text = base64

    println "Zpracováno: ${pdfFile.name} → ${outputFile.name}"
}