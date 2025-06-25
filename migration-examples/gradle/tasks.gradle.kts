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
        "postDeployReport", "migration report", "Generate post deploy report", "common.report.PostDeployReport"
    ),
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
        finalizedBy = "postDeployReport"
    ),
    MigTask(
        "deploySpecifiedDocumentObjects",
        "migration deploy",
        "Deploy document object ids",
        "common.DeployDocumentObjectIds",
        finalizedBy = "postDeployReport"
    ),
    MigTask(
        "exportDocumentObjectIds",
        "migration deploy",
        "Export document object ids to deploy",
        "common.ExportDocumentObjectIdsToDeploy"
    ),
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
        "docbookImport", "migration docbook", "Import docbook", "docbook.DocBookImport"
    ),
    MigTask(
        "exampleImport", "migration example", "Import example", "example.Import"
    ),
    MigTask(
        "acknowledgementLetterFromSource",
        "migration example",
        "acknowledgementLetterFromSource",
        "example.AcknowledgementLetterFromSource"
    ),
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