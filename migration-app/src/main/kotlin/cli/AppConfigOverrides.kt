package com.quadient.migration.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option

class AppConfigOverrides : OptionGroup(
    name = "application.conf overrides",
    help = "Override values of the app config file, applies to every command",
) {
    private val modulesDir by option("--modules-dir", help = "Directory with the modules")
    private val libsDir by option("--libs-dir", help = "Directory with additional libraries (.jar files) to be used by modules during execution")
    private val appDataDir by option("--app-data-dir", help = "Directory for app data like settings and job history")
    private val modulesDataDir by option("--modules-data-dir", help = "Directory for data written by the modules")
    private val loadGradleOnlyModules by option(
        "--load-gradle-only-modules",
        help = "Also load modules that target only Gradle",
    ).nullableFlag("--no-load-gradle-only-modules")

    fun toMap(): Map<String, String> = listOfNotNull(
        modulesDir?.let { "modules-dir" to it },
        libsDir?.let { "libs-dir" to it },
        appDataDir?.let { "app-data-dir" to it },
        modulesDataDir?.let { "modules-data-dir" to it },
        loadGradleOnlyModules?.let { "load-gradle-only-modules" to it.toString() },
    ).toMap()
}
