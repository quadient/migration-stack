package com.quadient.migration.service

import com.quadient.migration.log
import groovy.lang.GroovyClassLoader
import groovy.lang.GroovyResourceLoader
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.control.ClassNodeResolver
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SimpleMessage
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap

fun <T> withScriptClassLoader(
    libsDir: String,
    scriptDir: String,
    resourcesDir: String,
    block: (GroovyClassLoader) -> T,
): T {
    val jars = File(libsDir).listFiles { file -> file.isFile && file.extension == "jar" }?.sortedBy { it.name }.orEmpty()

    val jarUrls = jars.map { it.toURI().toURL() }.toTypedArray()

    return ChildFirstClassLoader(jarUrls, GroovyService::class.java.classLoader).use { libsClassLoader ->
        ScriptGroovyClassLoader(libsClassLoader, scriptDir, resourcesDir).use { classLoader ->
            val thread = Thread.currentThread()
            val previousContextClassLoader = thread.contextClassLoader
            thread.contextClassLoader = classLoader
            try {
                block(classLoader)
            } finally {
                thread.contextClassLoader = previousContextClassLoader
            }
        }
    }
}

private class ScriptGroovyClassLoader(parent: ClassLoader, scriptDir: String, resourcesDir: String) :
    GroovyClassLoader(parent, compilerConfig) {
    private val scriptsByType = ScriptTypes.byDeclaredType(File(scriptDir).canonicalFile, parent)

    init {
        addDir(scriptDir)
        addDir(resourcesDir)
        resourceLoader = GroovyResourceLoader { null }
    }

    private fun addDir(path: String) {
        val dir = File(path).absoluteFile
        if (!dir.isDirectory) {
            log.warn("Directory $dir does not exist, scripts cannot load files from it")
        }
        addURL(dir.toURI().toURL())
    }

    private fun scriptDeclaring(type: String, fail: (String) -> Unit = { error(it) }): File? {
        val scripts = scriptsByType[type].orEmpty()
        if (scripts.size > 1) {
            fail("Type $type is declared in more than one script: ${scripts.joinToString()}")
        }
        return scripts.firstOrNull()
    }

    override fun createCompilationUnit(config: CompilerConfiguration, source: CodeSource?): CompilationUnit {
        val unit = super.createCompilationUnit(config, source)
        unit.classNodeResolver = object : ClassNodeResolver() {
            override fun findClassNode(name: String, compilationUnit: CompilationUnit): LookupResult? {
                val compiledEarlier = getClassCacheEntry(name)
                val script = scriptDeclaring(name) { duplicate ->
                    compilationUnit.errorCollector.addFatalError(SimpleMessage(duplicate, compilationUnit))
                }
                return when {
                    compiledEarlier != null -> LookupResult(null, ClassHelper.make(compiledEarlier))
                    script != null -> LookupResult(compilationUnit.addSource(script), null)
                    else -> super.findClassNode(name, compilationUnit)
                }
            }
        }
        return unit
    }

    override fun loadClass(name: String, lookupScriptFiles: Boolean, preferClassOverScript: Boolean, resolve: Boolean): Class<*> {
        getClassCacheEntry(name)?.let { return it }

        val script = scriptDeclaring(name)
        if (lookupScriptFiles && script != null) {
            parseClass(script)
            getClassCacheEntry(name)?.let { return it }
        }

        return super.loadClass(name, lookupScriptFiles, preferClassOverScript, resolve)
    }
}

private val compilerConfig: CompilerConfiguration = CompilerConfiguration.DEFAULT

private object ScriptTypes {
    fun byDeclaredType(scriptDir: File, parent: ClassLoader): Map<String, List<File>> =
        GroovyClassLoader(parent).use { loader ->
            scriptDir.walkTopDown().filter { it.isFile && it.extension == "groovy" }.sortedBy { it.path }
                .flatMap { script -> declaredTypes(script, loader).map { type -> type to script } }
                .groupBy({ (type, _) -> type }, { (_, script) -> script })
        }

    private class ParsedScript(val lastModified: Long, val length: Long, val types: Set<String>)

    private val parsedScripts = ConcurrentHashMap<File, ParsedScript>()

    private fun declaredTypes(script: File, loader: GroovyClassLoader): Set<String> {
        val parsed = parsedScripts[script]
        if (parsed != null && parsed.lastModified == script.lastModified() && parsed.length == script.length()) {
            return parsed.types
        }

        val types = try {
            val source = SourceUnit(script, compilerConfig, loader, ErrorCollector(compilerConfig))
            source.parse()
            source.completePhase()
            source.convert()
            source.ast.classes.map { it.name }.toSet()
        } catch (e: CompilationFailedException) {
            log.warn("Types of script $script are not available to other scripts, it cannot be parsed: ${e.message}")
            emptySet()
        }
        parsedScripts[script] = ParsedScript(script.lastModified(), script.length(), types)
        return types
    }
}

class ChildFirstClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {
    init {
        log.debug("Loading script libraries: ${urls.joinToString()}")
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            val loaded = findLoadedClass(name) ?: findOwnClass(name) ?: parent.loadClass(name)
            if (resolve) {
                resolveClass(loaded)
            }
            return loaded
        }
    }

    override fun getResource(name: String): URL? = findResource(name) ?: parent.getResource(name)

    override fun getResources(name: String): Enumeration<URL> {
        return Collections.enumeration(findResources(name).toList() + parent.getResources(name).toList())
    }

    private fun findOwnClass(name: String): Class<*>? {
        if (name.startsWith("java.")) {
            return null
        }

        return try {
            findClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }
    }
}
