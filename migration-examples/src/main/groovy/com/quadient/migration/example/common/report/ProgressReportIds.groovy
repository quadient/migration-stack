package com.quadient.migration.example.common.report

import com.quadient.migration.example.common.util.ProgressReportWriter

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def documentObjects = Paths.get("deploy", "${migration.projectConfig.name}-document-objects")
    .toFile()
    .text
    .lines()
    .toList()

def report = migration.deployClient.progressReport(documentObjects, null)
ProgressReportWriter.writeProgressReport(report, migration.projectConfig.name)
