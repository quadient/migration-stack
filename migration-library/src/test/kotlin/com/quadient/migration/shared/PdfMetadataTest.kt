package com.quadient.migration.shared

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class PdfMetadataTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun `serialization with all fields`() {
        val metadata = PdfMetadata(
            title = "Test Title",
            author = "Test Author",
            subject = "Test Subject",
            keywords = "keyword1, keyword2",
            producer = "Test Producer"
        )

        val serialized = json.encodeToString(PdfMetadata.serializer(), metadata)
        val deserialized = json.decodeFromString(PdfMetadata.serializer(), serialized)

        assertEquals(metadata, deserialized)
    }

    @Test
    fun `serialization with null fields`() {
        val metadata = PdfMetadata(
            title = "Test Title",
            author = null,
            subject = null,
            keywords = null,
            producer = null
        )

        val serialized = json.encodeToString(PdfMetadata.serializer(), metadata)
        val deserialized = json.decodeFromString(PdfMetadata.serializer(), serialized)

        assertEquals("Test Title", deserialized.title)
        assertNull(deserialized.author)
        assertNull(deserialized.subject)
        assertNull(deserialized.keywords)
        assertNull(deserialized.producer)
    }

    @Test
    fun `serialization with all null fields (default)`() {
        val metadata = PdfMetadata()

        val serialized = json.encodeToString(PdfMetadata.serializer(), metadata)
        val deserialized = json.decodeFromString(PdfMetadata.serializer(), serialized)

        assertNull(deserialized.title)
        assertNull(deserialized.author)
        assertNull(deserialized.subject)
        assertNull(deserialized.keywords)
        assertNull(deserialized.producer)
    }

    @Test
    fun `roundtrip serialization`() {
        val original = PdfMetadata(
            title = "My Document",
            author = "John Doe"
        )

        val serialized = json.encodeToString(PdfMetadata.serializer(), original)
        val deserialized = json.decodeFromString(PdfMetadata.serializer(), serialized)

        assertEquals(original, deserialized)
    }
}
