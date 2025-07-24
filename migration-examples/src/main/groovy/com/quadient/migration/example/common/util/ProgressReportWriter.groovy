package com.quadient.migration.example.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.service.deploy.ReportedDocObject
import com.quadient.migration.service.deploy.ReportedImage
import java.nio.file.Paths
import com.quadient.migration.service.deploy.ProgressReport as Report

import java.time.Instant
import java.time.temporal.ChronoUnit

// id, name, type, doc object type?, internal, last status(created, kept, overwritten, inlined, error),
// next action(create, keep, overwrite, inline), next icm path, last icm path, error message, content,
// deploy id, deploy timestamp

static void writeProgressReport(Report report, String projectName) {
    def now = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS).toString()
            .replace('Z', '')
            .replace(':', '-')
    def dstFile = Paths.get("report", "${projectName}-progress-report-${now}.csv")

    def mapper = new ObjectMapper()
    def file = dstFile.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,type,document object type,internal,last status,next action,last icm path,next icm path,error,deploy id,deploy timestamp,content")
        for (item in report.items.values()) {
            switch (item) {
                case ReportedDocObject -> {
                    def obj = item.documentObject
                    def content = Csv.escapeJson(mapper.writeValueAsString(obj.content))
                    writer.write("$obj.id,") // Id
                    writer.write("${obj.name ?: ""},") // Name
                    writer.write("DocumentObject,") // Type
                    writer.write("${obj.type},") // Document Object Type
                    writer.write("${obj.internal},") // Internal
                    writer.write("${item.lastStatus.class.simpleName},") // Last Status
                    writer.write("${item.deployKind},") // Next Action
                    writer.write("${item.previousIcmPath ?: ""},") // Last ICM Path
                    writer.write("${item.nextIcmPath ?: ""},") // Next ICM Path
                    writer.write("${item.errorMessage?.replaceAll("\n", "")?.replaceAll(",", ";") ?: ""},") // Error Message
                    writer.write("${item.deploymentId ?: ""},") // Deploy ID
                    writer.write("${item.deployTimestamp ?: ""},") // Deploy Timestamp
                    writer.write("${content}") // Content
                    writer.writeLine("")
                }
                case ReportedImage -> {
                    def img = item.image
                    writer.write("$img.id,") // Id
                    writer.write("${img.name ?: ""},") // Name
                    writer.write("Image,") // Type
                    writer.write(",") // Document Object Type
                    writer.write(",") // Internal
                    writer.write("${item.lastStatus.class.simpleName},") // Last Status
                    writer.write("${item.deployKind},") // Next Action
                    writer.write("${item.previousIcmPath ?: ""},") // Last ICM Path
                    writer.write("${item.nextIcmPath ?: ""},") // Next ICM Path
                    writer.write("${item.errorMessage?.replaceAll("\n", "")?.replaceAll(",", ";") ?: ""},") // Error Message
                    writer.write("${item.deploymentId ?: ""},") // Deploy ID
                    writer.write("${item.deployTimestamp ?: ""},") // Deploy Timestamp
                    writer.write("") // Content
                    writer.writeLine("")
                }
                default -> ""
            }
        }
    }
}