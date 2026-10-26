package com.quadient.migration.service

import com.quadient.migration.log
import groovy.lang.GroovyClassLoader
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration

fun <T> withScriptClassLoader(libsDir: String, scriptDir: String, block: (GroovyClassLoader) -> T): T {
    val jars = File(libsDir).listFiles { file -> file.isFile && file.extension == "jar" }?.sortedBy { it.name }.orEmpty()

    val libsClassLoader = ChildFirstClassLoader(jars.map { it.toURI().toURL() }.toTypedArray(), GroovyService::class.java.classLoader)
    val classLoader = GroovyClassLoader(libsClassLoader).apply { addClasspath(scriptDir) }

    val thread = Thread.currentThread()
    val previousContextClassLoader = thread.contextClassLoader
    thread.contextClassLoader = classLoader
    try {
        return block(classLoader)
    } finally {
        thread.contextClassLoader = previousContextClassLoader
        classLoader.close()
        libsClassLoader.close()
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
