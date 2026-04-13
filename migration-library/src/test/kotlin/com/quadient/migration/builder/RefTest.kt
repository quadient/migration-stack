package com.quadient.migration.builder

import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RefTest {

    @Test
    fun `id cannot be empty`() {
        assertThrows<IllegalArgumentException>("Ref id can not be empty") { DocumentObjectRef("") }
    }

    @Test
    fun `id cannot be blank`() {
        assertThrows<IllegalArgumentException>("Ref id can not be blank") { DocumentObjectRef(" \t\n ") }
    }
}