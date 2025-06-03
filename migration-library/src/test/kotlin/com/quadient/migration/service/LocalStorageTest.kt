package com.quadient.migration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random

@TestInstance(PER_CLASS)
class LocalStorageTest {
    @field:TempDir
    lateinit var dir: File

    lateinit var storage: Storage

    @BeforeEach
    fun init() {
        storage = LocalStorage(dir.path, "project")
    }

    @AfterEach
    fun clean() {
        storage.deleteAll()
    }

    @Test
    fun `simple write string`() {
        val fileName = Paths.get("someDir", "file.txt").toString()
        val input = "hello world"

        storage.write(fileName, input)
        val result = storage.readAsString(fileName)

        assertEquals(input, result)
    }

    @Test
    fun `simple write bytes`() {
        val fileName = Paths.get("someDir", "file").toString()
        val input = Random.nextBytes(1_000)

        storage.write(fileName, input)
        val result = storage.read(fileName)

        assertEquals(input.count(), result.count())
        for ((resultByte, testByte) in input.zip(result)) {
            assertEquals(resultByte, testByte)
        }
    }

    @Test
    fun `simple write string in append mode`() {
        val fileName = Paths.get("someDir", "file.txt").toString()
        val input1 = "hello"
        val input2 = "world"

        storage.write(fileName, input1)
        storage.write(fileName, input2, WriteMode.APPEND)
        val result = storage.readAsString(fileName)

        assertEquals(input1 + input2, result)
    }

    @Test
    fun `simple write bytes in append mode`() {
        val fileName = Paths.get("someDir", "file").toString()
        val input1 = Random.nextBytes(1_000)
        val input2 = Random.nextBytes(1_000)

        storage.write(fileName, input1)
        storage.write(fileName, input2, WriteMode.APPEND)
        val result = storage.read(fileName)

        assertEquals((input1 + input2).count(), result.count())
        for ((resultByte, testByte) in (input1 + input2).zip(result)) {
            assertEquals(resultByte, testByte)
        }
    }

    @Test
    fun `file exists`() {
        val fileName = Paths.get("someDir", "file.txt").toString()
        val input = "hello world"

        storage.write(fileName, input)
        val result = storage.exists(fileName)

        assertTrue(result)
    }

    @Test
    fun `file deletes`() {
        val fileName = Paths.get("someDir", "file.txt").toString()
        val input = "hello world"

        storage.write(fileName, input)
        val result = storage.exists(fileName)

        assertTrue(result)

        val deleted = storage.delete(fileName)
        val exists2 = storage.exists(fileName)
        assertTrue(deleted)
        assertFalse(exists2)
    }

    @Test
    fun `delete all`() {
        val files = listOf(
            Paths.get("someDir", "file.txt"),
            Paths.get("someDir", "file2.txt"),
            Paths.get("someDir", "file3.txt"),
            Paths.get("file4.txt")
        ).map { it.toString() }
        val input = "hello world"

        files.forEach { storage.write(it, input) }
        files.forEach { assertTrue(storage.exists(it)) }
        assertEquals(storage.list().sorted().map { it.name }, listOf("someDir", "file4.txt").sorted())
        assertEquals(
            storage.list("someDir").sorted().map { it.name },
            listOf("file.txt", "file2.txt", "file3.txt").sorted()
        )

        storage.deleteAll()

        files.forEach { assertFalse(storage.exists(it)) }
    }
}