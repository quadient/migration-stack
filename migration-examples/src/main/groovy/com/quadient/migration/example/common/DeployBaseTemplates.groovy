//! ---
//! displayName: Deploy Base Templates
//! category: Deployment
//! description: Deploys all base templates
//! ---
package com.quadient.migration.example.common

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)
def start = System.currentTimeMillis()

deploymentResult = migration.deployClient.deployBaseTemplates()
@Field static Logger log = LoggerFactory.getLogger(this.class.name)

if (!deploymentResult.deployed.empty) {
    for (def item : deploymentResult.deployed) {
        log.info "Deployed ${item.type.toString()} '${item.id}' to '${item.targetPath}'"
    }
}

if (!deploymentResult.warnings.empty) {
    for (def item : deploymentResult.warnings) {
        log.warn "Item '${item.id}' deployed with warning: ${item.message}"
    }
}

if (!deploymentResult.errors.empty) {
    for (def item : deploymentResult.errors) {
        log.error "Item '${item.id}' failed to deploy with error: ${item.message}"
    }
}
log.info "Deployment finished. Deployed ${deploymentResult.deployed.size()} items with ${deploymentResult.warnings.size()} warnings and ${deploymentResult.errors.size()} errors"
log.info "Deployment took ${System.currentTimeMillis() - start} ms"
