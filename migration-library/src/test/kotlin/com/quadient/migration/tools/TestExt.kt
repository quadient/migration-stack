package com.quadient.migration.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat
import org.testcontainers.shaded.org.hamcrest.Matchers.equalTo
import org.testcontainers.shaded.org.hamcrest.Matchers.greaterThan
import org.testcontainers.shaded.org.hamcrest.Matchers.not

fun <T> Collection<T>.shouldBeEmpty() = assertEquals(this.count(), 0)
fun <T> Collection<T>.shouldBeOfSize(size: Int) = assertEquals(size, this.count())

fun <T> T.shouldBeEqualTo(expected: T) = assertEquals(expected, this)
fun <T> T.shouldNotBeEqualTo(expected: T) = assertEquals(true, this != expected)

fun ByteArray?.shouldBeEqualTo(expected: ByteArray) = assertTrue(expected.contentEquals(this), "Expected byte arrays to be equal")
fun ByteArray?.shouldNotBeEqualTo(expected: ByteArray) = assertFalse(expected.contentEquals(this), "Expected byte arrays to not be equal")

fun <T> T?.shouldBeNull() = assertEquals(null, this)
fun <T> T?.shouldNotBeNull() = assertThat(this, not(equalTo(null)))

fun <T> T.shouldBeOfInstance(clazz: Class<*>) = assertInstanceOf(clazz, this,  "Expected instance of ${clazz.simpleName} but was ${this?.javaClass?.simpleName}")
inline fun <reified T> Any?.shouldBeOfInstance() = assertTrue(this is T, "Expected instance of ${T::class.simpleName} but was ${this?.javaClass?.simpleName}")

fun CharSequence?.shouldNotBeEmpty() = assertThat("Expected sequence '$this' to be not empty", this?.length ?: 0, greaterThan(0))

fun String?.shouldStartWith(prefix: String) = assertTrue(this != null && this.startsWith(prefix), "Expected string to start with '$prefix' but was '$this'")
