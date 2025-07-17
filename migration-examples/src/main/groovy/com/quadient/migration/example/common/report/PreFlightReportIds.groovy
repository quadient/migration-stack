package com.quadient.migration.example.common.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.service.deploy.DeployedDocumentObject
import com.quadient.migration.service.deploy.DeployedImage
import com.quadient.migration.service.deploy.DeployedParagraphStyle
import com.quadient.migration.service.deploy.DeployedTextStyle
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def documentObjects = Paths.get("deploy", "${migration.projectConfig.name}-document-objects")
    .toFile()
    .text
    .lines()
    .toList()

def result = migration.deployClient.preFlightCheck(documentObjects)

def dstFile = Paths.get("report", "${migration.projectConfig.name}-pre-flight-report.csv")

def mapper = new ObjectMapper()
def file = dstFile.toFile()
file.createParentDirectories()
file.withWriter { writer ->
    writer.writeLine("id,name,type,deployedAs,internal,icmPath,content")
    for (item in result.items.values()) {
        def line = switch (item) {
            case DeployedDocumentObject -> {
                def obj = item.documentObject
                def content = Csv.escapeJson(mapper.writeValueAsString(obj.content))
                "${obj.id},${obj.name},${obj.type},${item.deployKind},${obj.internal},${item.icmPath},${content}"
            }
            case DeployedImage -> {
                "${item.id},${item.image.name},Image,${item.deployKind},null,${item.icmPath},null"
            }
            case DeployedTextStyle -> {
                def content = Csv.escapeJson(mapper.writeValueAsString(item.style.definition))
                "${item.id},${item.style.name},TextStyle,${item.deployKind},null,${item.icmPath},${content}"
            }
            case DeployedParagraphStyle -> {
                def content = Csv.escapeJson(mapper.writeValueAsString(item.style.definition))
                "${item.id},${item.style.name},ParagraphStyle,${item.deployKind},null,${item.icmPath},${content}"
            }
            default -> ""
        }
        writer.writeLine(line)
    }
}