//! ---
//! displayName: Post-deploy Status Tracking Report
//! category: Report
//! ---
package com.quadient.migration.example.common.report

import com.quadient.migration.api.Migration
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.example.common.util.PathUtil
import groovy.transform.Field

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

def dstFile = PathUtil.dataDirPath(binding, "report", "${migration.projectConfig.name}-status-tracking-report.csv")

def objects = migration.statusTrackingRepository.listAll()

def file =  dstFile.toFile()
file.createParentDirectories()
file.withWriter { writer ->
    writer.writeLine("id,resourceType,status,timestamp,deploymentId,icmPath,errorMessage,data")
    for (object in objects) {
        def lastEvent = object.statusEvents.last()
        switch (lastEvent) {
            case Active:
                writer.writeLine("\"$object.id\",$object.resourceType,Active,${lastEvent.timestamp},,,,${lastEvent.data.collect { "$it.key:$it.value" }.join("; ")}")
                break
            case Deployed:
                writer.writeLine("\"$object.id\",$object.resourceType,Deployed,${lastEvent.timestamp},${lastEvent.deploymentId},${lastEvent.icmPath},,${lastEvent.data.collect { "$it.key:$it.value" }.join("; ")}")
                break
            case com.quadient.migration.data.Error:
                def sanitizedError = lastEvent.error
                    .replaceAll("[\n\r]", " ")
                    .replaceAll("\"", "'")
                writer.writeLine("\"$object.id\",$object.resourceType,Error,${lastEvent.timestamp},${lastEvent.deploymentId},${lastEvent.icmPath},\"${sanitizedError}\",,${lastEvent.data.collect { "$it.key:$it.value" }.join("; ")}")
                break
        }
    }
}
