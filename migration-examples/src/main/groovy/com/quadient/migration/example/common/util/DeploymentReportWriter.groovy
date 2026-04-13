package com.quadient.migration.example.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.service.deploy.ReportedDisplayRule
import com.quadient.migration.service.deploy.ReportedDocObject
import com.quadient.migration.service.deploy.ReportedFile
import com.quadient.migration.service.deploy.ReportedImage
import com.quadient.migration.service.deploy.ProgressReport as Report

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// id, name, type, doc object type?, internal, last status(created, kept, overwritten, inlined, error),
// next action(create, keep, overwrite, inline), next icm path, last icm path, error message, content,
// deploy id, deploy timestamp

static void writeDeploymentReport(Binding binding, Report report, String projectName) {
    def now = LocalDateTime.now()
    def filenameFriendly = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"))
    def dstFile  = PathUtil.dataDirPath(binding, "report", "${projectName}-deployment-report-${filenameFriendly}.csv")

    def mapper = new ObjectMapper()
    def file = dstFile.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,type,document object type,internal,last status,next action,last icm path,next icm path,error,deploy id,deploy timestamp,content")
        for (item in report.items.values()) {
            switch (item) {
                case ReportedDocObject -> {
                    def obj = item.documentObject
                    def content = obj ? Csv.escapeJson(mapper.writeValueAsString(obj.content)) : ""
                    writer << Csv.serialize(item.id) + "," // Id
                    writer << Csv.serialize(obj?.name) + "," // Name
                    writer << Csv.serialize("DocumentObject") + "," // Type
                    writer << Csv.serialize(obj?.type) + "," // Document Object Type
                    writer << Csv.serialize(obj?.internal) + "," // Internal
                    writer << Csv.serialize(item.lastStatus.class.simpleName) + "," // Last Status
                    writer << Csv.serialize(item.deployKind) + "," // Next Action
                    writer << Csv.serialize(item.previousIcmPath) + "," // Last ICM Path
                    writer << Csv.serialize(item.nextIcmPath) + "," // Next ICM Path
                    writer << Csv.serialize(item.errorMessage?.replaceAll("[\r\n]+", "")?.replaceAll(",", ";")) + "," // Error Message
                    writer << Csv.serialize(item.deploymentId) + "," // Deploy ID
                    writer << Csv.serialize(item.deployTimestamp) + "," // Deploy Timestamp
                    writer << "${content}" // Content
                    writer << ("\n")
                }
                case ReportedImage -> {
                    def img = item.image
                    writer << Csv.serialize(item.id) + "," // Id
                    writer << Csv.serialize(img?.name) + "," // Name
                    writer << Csv.serialize("Image") + "," // Type
                    writer << Csv.serialize("") + "," // Document Object Type
                    writer << Csv.serialize("") + "," // Internal
                    writer << Csv.serialize(item.lastStatus.class.simpleName) + "," // Last Status
                    writer << Csv.serialize(item.deployKind) + "," // Next Action
                    writer << Csv.serialize(item.previousIcmPath) + "," // Last ICM Path
                    writer << Csv.serialize(item.nextIcmPath) + "," // Next ICM Path
                    writer << Csv.serialize(item.errorMessage?.replaceAll("[\r\n]+", "")?.replaceAll(",", ";")) + "," // Error Message
                    writer << Csv.serialize(item.deploymentId) + "," // Deploy ID
                    writer << Csv.serialize(item.deployTimestamp) + "," // Deploy Timestamp
                    writer << Csv.serialize("") // Content
                    writer << ("\n")
                }
                case ReportedFile -> {
                    def fileItem = item.attachment
                    writer << Csv.serialize(item.id) + "," // Id
                    writer << Csv.serialize(fileItem?.name) + "," // Name
                    writer << Csv.serialize("File") + "," // Type
                    writer << Csv.serialize("") + "," // Document Object Type
                    writer << Csv.serialize("") + "," // Internal
                    writer << Csv.serialize(item.lastStatus.class.simpleName) + "," // Last Status
                    writer << Csv.serialize(item.deployKind) + "," // Next Action
                    writer << Csv.serialize(item.previousIcmPath) + "," // Last ICM Path
                    writer << Csv.serialize(item.nextIcmPath) + "," // Next ICM Path
                    writer << Csv.serialize(item.errorMessage?.replaceAll("[\r\n]+", "")?.replaceAll(",", ";")) + "," // Error Message
                    writer << Csv.serialize(item.deploymentId) + "," // Deploy ID
                    writer << Csv.serialize(item.deployTimestamp) + "," // Deploy Timestamp
                    writer << Csv.serialize("") +"," // Content
                    writer << ("\n")
                }
                case ReportedDisplayRule -> {
                    def rule = item.displayRule
                    def content = rule ? Csv.escapeJson(mapper.writeValueAsString(rule.definition)) : ""
                    writer << Csv.serialize(item.id) + "," // Id
                    writer << Csv.serialize(rule?.name) + "," // Name
                    writer << Csv.serialize("DisplayRule") + "," // Type
                    writer << Csv.serialize("") + "," // Document Object Type
                    writer << Csv.serialize(rule?.internal) + "," // Internal
                    writer << Csv.serialize(item.lastStatus.class.simpleName) + "," // Last Status
                    writer << Csv.serialize(item.deployKind) + "," // Next Action
                    writer << Csv.serialize(item.previousIcmPath) + "," // Last ICM Path
                    writer << Csv.serialize(item.nextIcmPath) + "," // Next ICM Path
                    writer << Csv.serialize(item.errorMessage?.replaceAll("[\r\n]+", "")?.replaceAll(",", ";")) + "," // Error Message
                    writer << Csv.serialize(item.deploymentId) + "," // Deploy ID
                    writer << Csv.serialize(item.deployTimestamp) + "," // Deploy Timestamp
                    writer << Csv.serialize(content) + "," // Content
                    writer << ("\n")
                }
                default -> ""
            }
        }
    }
}
