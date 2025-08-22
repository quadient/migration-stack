//! ---
//! category: Deployment
//! description: Deploys selected document objects
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.DeploymentReportWriter

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def documentObjects = Paths.get("deploy", "${migration.projectConfig.name}-document-objects")
        .toFile()
        .text
        .lines()
        .toList()

migration.deployClient.deployDocumentObjects(documentObjects, false)

def report = migration.deployClient.progressReport(documentObjects, null)
DeploymentReportWriter.writeDeploymentReport(report, migration.projectConfig.name)