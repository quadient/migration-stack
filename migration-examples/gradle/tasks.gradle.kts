import java.io.InputStream

data class MigTask(
    val name: String,
    val group: String,
    val description: String,
    val mainClassRelativePath: String,
    val stdIn: InputStream? = null,
    val finalizedBy: String? = null,
)

val migrationTasks = listOf<MigTask>(
    // Migration import
    MigTask(
        "modelComplexityReport", "migration report", "Generate complexity report", "common.report.ComplexityReport"
    ),
    MigTask(
        "displayRuleReport", "migration report", "Generate display rule report", "common.report.DisplayRuleReport"
    ),
    MigTask(
        "postDeployStatusTrackingReport",
        "migration report",
        "Generate post deploy report",
        "common.report.PostDeployStatusTrackingReport"
    ),
    MigTask(
        "progressReport", "migration report", "Generate progress report", "common.report.ProgressReport"
    ),
    MigTask(
        "progressReportIds", "migration report", "Generate progress report", "common.report.ProgressReportIds"
    ),

    // Migration deploy
    MigTask(
        "validateAll", "migration deploy", "Run validation on all migration objects", "common.Validate"
    ),
    MigTask(
        "deployStyles", "migration deploy", "Deploy styles", "common.DeployStyles"
    ),
    MigTask(
        "deployAllDocumentObjects",
        "migration deploy",
        "Deploy document objects",
        "common.DeployDocumentObjects",
    ),
    MigTask(
        "deploySpecifiedDocumentObjects",
        "migration deploy",
        "Deploy document object ids",
        "common.DeployDocumentObjectIds",
    ),
    MigTask(
        "exportDocumentObjectIds",
        "migration deploy",
        "Export document object ids to deploy",
        "common.ExportDocumentObjectIdsToDeploy"
    ),

    // Migration mapping
    MigTask(
        "paragraphStylesExport", "migration mapping", "Export paragraph styles", "common.mapping.ParagraphStylesExport"
    ),
    MigTask(
        "textStylesExport", "migration mapping", "Export text styles", "common.mapping.TextStylesExport"
    ),
    MigTask(
        "variablesExport", "migration mapping", "Export variables", "common.mapping.VariablesExport"
    ),
    MigTask(
        "documentObjectsImagesExport",
        "migration mapping",
        "Export document objects and images",
        "common.mapping.DocumentObjectsImagesExport"
    ),
    MigTask(
        "areasExport",
        "migration mapping",
        "Export areas assigned to the appropriate pages and templates. Allows user to modify interactive flow name",
        "common.mapping.AreasExport"
    ),
    MigTask(
        "paragraphStylesImport", "migration mapping", "Import paragraph styles", "common.mapping.ParagraphStylesImport"
    ),
    MigTask(
        "textStylesImport", "migration mapping", "Import text styles", "common.mapping.TextStylesImport"
    ),
    MigTask(
        "variablesImport", "migration mapping", "Import variables", "common.mapping.VariablesImport"
    ),
    MigTask(
        "documentObjectsImagesImport",
        "migration mapping",
        "Import document objects and images",
        "common.mapping.DocumentObjectsImagesImport"
    ),
    MigTask(
        "areasImport",
        "migration mapping",
        "Import areas with modified interactive flow names to their respective pages",
        "common.mapping.AreasImport"
    ),

    // Migration docbook
    MigTask(
        "docbookImport", "migration docbook", "Import docbook", "docbook.DocBookImport"
    ),

    // Migration example
    MigTask(
        "exampleImport", "migration example", "Import example", "example.Import"
    ),
    MigTask(
        "acknowledgementLetterFromSource",
        "migration example",
        "acknowledgementLetterFromSource",
        "example.AcknowledgementLetterFromSource"
    ),

    // Migration common
    MigTask(
        "destroy", "migration common", "Destroy database and storage", "common.Destroy", System.`in`
    ),
    MigTask(
        "activateAll", "migration common", "Make all objects Active", "common.ActivateAll"
    ),
)

tasks {
    migrationTasks.forEach {
        register<JavaExec>(it.name) {
            description = it.description
            group = it.group
            mainClass = "com.quadient.migration.example.${it.mainClassRelativePath}"
            classpath = project.extensions.getByType<JavaPluginExtension>().sourceSets["main"].runtimeClasspath
            args = project.findProperty("scriptArgs")?.toString()?.split(" ") ?: emptyList()
            if (it.stdIn != null) {
                standardInput = it.stdIn
            }
            if (it.finalizedBy != null) {
                finalizedBy(it.finalizedBy)
            }
        }
    }
}