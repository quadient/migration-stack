package com.quadient.migration.service.ipsclient

import com.quadient.migration.api.IpsConfig
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IpsServiceTest {
    private val subject = spyk(IpsService(IpsConfig()))
    private val baseTemplate = "icm://BaseTemplate.wfd".toIcmPath()

    @Test
    fun `extractJld extracts to memory, downloads the jld and removes the memory location`() {
        val memoryLocation = slot<String>()
        every { subject.deployJld(baseTemplate, "Template", "DocumentLayout", "<xml/>", capture(memoryLocation)) } returns
            OperationResult.Success
        every { subject.download(any<String>()) } returns byteArrayOf(1, 2, 3)
        every { subject.delete(any<String>()) } returns true

        val result = subject.extractJld(baseTemplate, "Template", "<xml/>")

        result.toList().shouldBeEqualTo(listOf<Byte>(1, 2, 3))
        memoryLocation.captured.startsWith("memory://").shouldBeEqualTo(true)
        verify { subject.download(memoryLocation.captured) }
        verify { subject.delete(memoryLocation.captured) }
    }

    @Test
    fun `extractJld fails with the deployment message and removes the memory location`() {
        every { subject.deployJld(baseTemplate, any(), any(), any(), any<String>()) } returns OperationResult.Failure("IPS deploy failed")
        every { subject.delete(any<String>()) } returns true

        val exception = assertThrows<IpsClientException> { subject.extractJld(baseTemplate, "Template", "<xml/>") }

        exception.message.shouldBeEqualTo("IPS deploy failed")
        verify(exactly = 0) { subject.download(any<String>()) }
        verify { subject.delete(match<String> { it.startsWith("memory://") }) }
    }

    @Test
    fun `extractJld fails when the download fails and removes the memory location`() {
        every { subject.deployJld(baseTemplate, any(), any(), any(), any<String>()) } returns OperationResult.Success
        every { subject.download(any<String>()) } throws RuntimeException("download error")
        every { subject.delete(any<String>()) } returns true

        val exception = assertThrows<IpsClientException> { subject.extractJld(baseTemplate, "Template", "<xml/>") }

        exception.message!!.startsWith("Failed to download deployed JLD").shouldBeEqualTo(true)
        verify { subject.delete(match<String> { it.startsWith("memory://") }) }
    }
}
