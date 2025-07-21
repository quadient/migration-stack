package com.quadient.migration.tools

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

fun <T> Collection<T>.shouldBeEmpty() = assertEquals(this.count(), 0)
fun <T> Collection<T>.shouldBeOfSize(size: Int) = assertEquals(size, this.count())

fun <T> T.shouldBeEqualTo(expected: T) = assertEquals(expected, this)
fun <T> T.shouldNotBeEqualTo(expected: T) = assertEquals(true, this != expected)

fun <T> T?.shouldBeNull() = assertEquals(null, this)
fun <T> T?.shouldNotBeNull() = assertThat(this, not(equalTo(null)))

fun String?.shouldStartWith(prefix: String) = assertTrue(this != null && this.startsWith(prefix), "Expected string to start with '$prefix' but was '$this'")
