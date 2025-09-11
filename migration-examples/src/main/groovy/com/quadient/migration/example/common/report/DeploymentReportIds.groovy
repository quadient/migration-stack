//! ---
//! displayName: Specified Objects Deployment Report
//! category: Report
//! description: Creates a CSV report with deployment progress and status information for the migration project. Only the IDs listed in the 'deploy/<project-name>-document-objects' are taken into account.
//! ---
package com.quadient.migration.example.common.report

import com.quadient.migration.example.common.util.DeploymentReportWriter

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def documentObjects = Paths.get("deploy", "${migration.projectConfig.name}-document-objects")
    .toFile()
    .text
    .lines()
    .toList()

def report = migration.deployClient.progressReport(documentObjects, null)
DeploymentReportWriter.writeDeploymentReport(binding, report, migration.projectConfig.name)
