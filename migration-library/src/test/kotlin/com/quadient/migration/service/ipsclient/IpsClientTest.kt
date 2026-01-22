package com.quadient.migration.service.ipsclient

import com.quadient.migration.Ips
import com.quadient.migration.shared.IcmDateTime
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.MetadataValue
import com.quadient.migration.tools.aMigConfig
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

// @Ips
@Disabled
class IpsClientTest {
    val client = IpsClient(Ips.Companion.HOST, Ips.Companion.PORT, 120.seconds)
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
    fun `writemetadata`() {
        val input = mutableMapOf(
            "TestKey" to MetadataValue(
                values = listOf(
                    MetadataPrimitive.DateTime(IcmDateTime(Clock.System.now())),
                ),
            )
        )
        service.writeMetadata(
            path = "icm://Interactive/StandardPackage/BaseTemplates/EUAddressLetterheadBaseTemplate.wfd",
            metadata = input
        )

        val result =
            service.readMetadata("icm://Interactive/StandardPackage/BaseTemplates/EUAddressLetterheadBaseTemplate.wfd")

        Assertions.assertEquals(result.metadata["TestKey"], input["TestKey"])
        println()
    }

    @Test
    fun `file upload round trip`() {
        val inputData = Random.Default.nextBytes(1_000_000)
        val uploadPath = "memory://test"

        client.upload(uploadPath, inputData).throwIfNotOk()
        client.download(uploadPath).throwIfNotOk().ifOk {
            Assertions.assertEquals(it.customData.count(), inputData.count())
            for ((resultByte, testByte) in it.customData.zip(inputData)) {
                Assertions.assertEquals(resultByte, testByte)
            }
        }
    }
}