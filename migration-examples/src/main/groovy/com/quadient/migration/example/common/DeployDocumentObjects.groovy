//! ---
//! category: Deployment
//! description: Deploys all document objects
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.DeploymentReportWriter
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)
deploymentResult = migration.deployClient.deployDocumentObjects()
@Field static Logger log = LoggerFactory.getLogger(this.class.name)

if (deploymentResult.errors.size() > 0) {
    log.error "Deployment errors: [${deploymentResult.errors.collect { error -> println "  ${error.id} - ${error.message}" }.join("\n")}]"
} else {
    log.info "No errors during deployment."
}

def report = migration.deployClient.progressReport(null)
DeploymentReportWriter.writeDeploymentReport(binding, report, migration.projectConfig.name)
