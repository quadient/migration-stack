package com.quadient.migration.service

import com.quadient.migration.Ips
import com.quadient.migration.service.ipsclient.IpsClient
import com.quadient.migration.service.ipsclient.IpsResult
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.tools.aMigConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

// @Ips
@Disabled
class IpsClientTest {
    val client = IpsClient(Ips.HOST, Ips.PORT, 120.seconds)
    val service = IpsService(aMigConfig().inspireConfig.ipsConfig)

    @Test
    fun `ping works`() {
        val result = client.ping()

        assert(result is IpsResult.Ok)
    }

    @Test
    fun `metadata`() {
        val result = service.readMetadata(
            listOf(
                "icm://Interactive/StandardPackage/BaseTemplates/EUAddressLetterheadBaseTemplate.wfd",
                "icm://Interactive/StandardPackage/BaseTemplates/FormBaseTemplate.wfd",
            )
        )

        println()
    }

    @Test
    fun `file upload round trip`() {
        val inputData = Random.nextBytes(1_000_000)
        val uploadPath = "memory://test"

        client.upload(uploadPath, inputData).throwIfNotOk()
        client.download(uploadPath).throwIfNotOk().ifOk {
                assertEquals(it.customData.count(), inputData.count())
                for ((resultByte, testByte) in it.customData.zip(inputData)) {
                    assertEquals(resultByte, testByte)
                }
            }
    }
}