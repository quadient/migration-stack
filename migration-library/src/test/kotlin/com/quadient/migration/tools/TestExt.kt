package com.quadient.migration.tools

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat
import org.testcontainers.shaded.org.hamcrest.Matchers.greaterThan

fun <T> Collection<T>.shouldBeEmpty() = assertEquals(this.count(), 0)
fun <T> Collection<T>.shouldBeOfSize(size: Int) = assertEquals(size, this.count())

fun <T> T.shouldBeEqualTo(expected: T) = assertEquals(expected, this)
fun <T> T.shouldNotBeEqualTo(expected: T) = assertEquals(true, this != expected)

fun <T> T?.shouldBeNull() = assertEquals(null, this)
fun <T> T?.shouldNotBeNull() = assertThat(this, not(equalTo(null)))

fun CharSequence?.shouldNotBeEmpty() = assertThat("Expected sequence '$this' to be not empty", this?.length ?: 0, greaterThan(0))

fun String?.shouldStartWith(prefix: String) = assertTrue(this != null && this.startsWith(prefix), "Expected string to start with '$prefix' but was '$this'")
