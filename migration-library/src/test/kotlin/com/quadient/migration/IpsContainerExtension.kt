package com.quadient.migration

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Target(AnnotationTarget.CLASS)
@ExtendWith(Ips.IpsExtension::class)
annotation class Ips {
    companion object {
        var PORT = 30354
        var HOST = "localhost"
    }

    @Testcontainers
    class IpsExtension : BeforeAllCallback, AfterAllCallback {

        @Container
        val ips = GenericContainer(DockerImageName.parse("quadient.azurecr.io/inspire/ips:16.6.411.0-FMAP"))
            .withEnv("CX_LIC", System.getenv("CX_LIC"))
            .withEnv("CX_LIC_SERVER", System.getenv("CX_LIC_SERVER"))
            .withExposedPorts(30354)

        override fun beforeAll(p0: ExtensionContext?) {
            ips.start()
            PORT = ips.getMappedPort(30354)
            HOST = ips.host
        }

        override fun afterAll(p0: ExtensionContext?) {
            ips.stop()
        }
    }
}
