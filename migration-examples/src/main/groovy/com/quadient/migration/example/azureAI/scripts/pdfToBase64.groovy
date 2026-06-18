package com.quadient.migration.example.azureAI.scripts

import com.quadient.migration.api.Migration
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

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

    log.info "Zpracováno: ${pdfFile.name} → ${outputFile.name}"
}