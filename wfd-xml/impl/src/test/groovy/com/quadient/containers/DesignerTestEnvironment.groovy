package com.quadient.containers

import com.quadient.wfdxml.generator.WfdGenerator
import spock.lang.Shared
import spock.lang.Specification

class DesignerTestEnvironment extends Specification {

    @Shared
    protected IpsDockerCompose ipsDockerCompose = new IpsDockerCompose()
            .withExposedIcm()
            .withExposedIps()
            .withLocalCompose(true)

    def setupSpec() {
        ipsDockerCompose.start()
    }

    def cleanupSpec() {
        ipsDockerCompose.stop()
    }

    protected WfdGenerator getWfdIpsGenerator() {
        return new WfdGenerator(ipsDockerCompose.geIpsHost(), ipsDockerCompose.getIpsPort())
    }
}