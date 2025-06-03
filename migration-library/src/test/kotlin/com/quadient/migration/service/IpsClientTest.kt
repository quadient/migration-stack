package com.quadient.migration.service

import com.quadient.migration.Ips
import com.quadient.migration.service.ipsclient.IpsClient
import com.quadient.migration.service.ipsclient.IpsResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Ips
class IpsClientTest {
    val client = IpsClient(Ips.HOST, Ips.PORT, 120.seconds)

    @Test
    fun `ping works`() {
        val result = client.ping()

        assert(result is IpsResult.Ok)
    }

    @Test
    fun `file upload round trip`() {
        val inputData = Random.nextBytes(1_000_000)
        val uploadPath = "memory://test"

        client.upload(uploadPath, inputData).throwIfNotOk()
        client.download(uploadPath)
            .throwIfNotOk()
            .ifOk {
                assertEquals(it.customData.count(), inputData.count())
                for ((resultByte, testByte) in it.customData.zip(inputData)) {
                    assertEquals(resultByte, testByte)
                }
            }
    }
}