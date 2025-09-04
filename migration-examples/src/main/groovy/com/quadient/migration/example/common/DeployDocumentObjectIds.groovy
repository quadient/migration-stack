//! ---
//! category: Deployment
//! description: Deploys selected document objects
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.DeploymentReportWriter
import com.quadient.migration.example.common.util.PathUtil

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def documentObjects = PathUtil.dataDirPath(binding, "deploy", "${migration.projectConfig.name}-document-objects")
        .toFile()
        .text
        .lines()
        .toList()

migration.deployClient.deployDocumentObjects(documentObjects, false)

def report = migration.deployClient.progressReport(documentObjects, null)
DeploymentReportWriter.writeDeploymentReport(binding, report, migration.projectConfig.name)