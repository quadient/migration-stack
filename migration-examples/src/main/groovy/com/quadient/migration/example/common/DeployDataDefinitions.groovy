//! ---
//! displayName: Deploy Data Definitions
//! category: Deployment
//! description: Deploys one data definition per variable structure to Interactive
//! ---
package com.quadient.migration.example.common

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)
def migration = initMigration(this.binding)
deploymentResult = migration.deployClient.deployDataDefinitions()
for (def item : deploymentResult.deployed) {
    log.info "Deployed data definition '${item.id}' to '${item.targetPath}'"
}
for (def item : deploymentResult.warnings) {
    log.warn "Data definition '${item.id}' deployed with warning: ${item.message}"
}
for (def item : deploymentResult.errors) {
    log.error "Data definition '${item.id}' failed to deploy: ${item.message}"
}
log.info "Deployment finished. Deployed ${deploymentResult.deployed.size()} data definitions with ${deploymentResult.warnings.size()} warnings and ${deploymentResult.errors.size()} errors"
