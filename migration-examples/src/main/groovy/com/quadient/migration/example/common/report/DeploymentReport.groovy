//! ---
//! category: Report
//! ---
package com.quadient.migration.example.common.report

import com.quadient.migration.example.common.util.DeploymentReportWriter

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)
def report = migration.deployClient.progressReport(null)

DeploymentReportWriter.writeDeploymentReport(report, migration.projectConfig.name)
