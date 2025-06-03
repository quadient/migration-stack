import java.io.InputStream

data class MigTask(
    val name: String,
    val group: String,
    val description: String,
    val mainClassRelativePath: String,
    val stdIn: InputStream? = null,
)

val migrationTasks = listOf<MigTask>(
    MigTask(
        "displayRuleReport", "migration report", "Generate display rule report", "common.report.DisplayRuleReport"
    ),
    MigTask(
        "validateAll", "migration deploy", "Run validation on all migration objects", "common.Validate"
    ),
    MigTask(
        "deployStyles", "migration deploy", "Deploy styles", "common.DeployStyles"
    ),
    MigTask(
        "deployAllDocumentObjects", "migration deploy", "Deploy document objects", "common.DeployDocumentObjects"
    ),
    MigTask(
        "exportDocumentObjectIds",
        "migration deploy",
        "Export document object ids to deploy",
        "common.ExportDocumentObjectIdsToDeploy"
    ),
    MigTask(
        "deploySpecifiedDocumentObjects",
        "migration deploy",
        "Deploy document object ids",
        "common.DeployDocumentObjectIds"
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
        "destroy", "migration common", "Destroy database and storage", "common.Destroy", System.`in`
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
        }
    }
}