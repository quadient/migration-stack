package com.quadient.migration.service

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths

enum class WriteMode {
    APPEND, OVERWRITE
}

interface Storage {
    fun openRead(path: String): InputStream
    fun openWrite(path: String, writeMode: WriteMode): OutputStream
    fun exists(path: String): Boolean
    fun delete(path: String): Boolean
    fun deleteAll()
    fun list(dirPath: String): List<File>

    fun read(path: String): ByteArray = openRead(path).use { it.readAllBytes() }
    fun readAsString(path: String): String = openRead(path).use { it.reader().use { it.readText() }}
    fun list(): List<File> = list("")

    fun openWrite(path: String) = openWrite(path, WriteMode.OVERWRITE)
    fun write(path: String, content: String) = openWrite(path, WriteMode.OVERWRITE).use { it.write(content.toByteArray()) }
    fun write(path: String, content: String, writeMode: WriteMode) = openWrite(path, writeMode).use { it.write(content.toByteArray()) }
    fun write(path: String, data: ByteArray) = openWrite(path, WriteMode.OVERWRITE).use { it.write(data) }
    fun write(path: String, data: ByteArray, writeMode: WriteMode) = openWrite(path, writeMode).use { it.write(data) }
}

class LocalStorage(private val storageRoot: String, private val projectName: String) : Storage {
    override fun openRead(path: String): InputStream {
        return path.toFile().inputStream()
    }

    override fun openWrite(path: String, writeMode: WriteMode): OutputStream {
        return path.toFile().run {
            parentFile.mkdirs()
            FileOutputStream(this, when (writeMode) {
                WriteMode.APPEND -> true
                WriteMode.OVERWRITE -> false
            })
        }
    }

    override fun exists(path: String): Boolean {
        return path.toFile().exists()
    }

    override fun delete(path: String): Boolean {
        return path.toFile().delete()
    }

    override fun deleteAll() {
        deleteDir("dummy".toFile().parentFile)
    }

    override fun list(dirPath: String): List<File> {
        return dirPath.toFile().listFiles()?.toList() ?: emptyList()
    }

    private fun deleteDir(dir: File) {
        if (dir.exists() && dir.isDirectory) {
            for (entry in dir.listFiles()) {
                if (entry.isDirectory) {
                    deleteDir(entry)
                } else {
                    entry.delete()
                }
            }
            dir.delete()
        }
    }

    private fun String.toFile() = File(Paths.get(storageRoot, projectName, this).toString())
}
