package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.DeploymentReportWriter

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])
def deploymentResult = migration.deployClient.deployDocumentObjects()

if (deploymentResult.errors.size() > 0) {
    println "Deployment errors: ["
    deploymentResult.errors.each { error -> println "  ${error.id} - ${error.message}" }
    println "]"
} else {
    println "No errors during deployment."
}

def report = migration.deployClient.progressReport(null)
DeploymentReportWriter.writeDeploymentReport(report, migration.projectConfig.name)
