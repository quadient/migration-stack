package com.quadient.migration.example.common.report

import com.quadient.migration.example.common.util.ProgressReportWriter

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])
def report = migration.deployClient.progressReport()

ProgressReportWriter.writeProgressReport(report, migration.projectConfig.name)
