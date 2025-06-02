package com.quadient.containers

import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerState
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.ThrowingFunction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class IpsDockerCompose extends ComposeContainer {

    private static final DESIGNER_DOCKER_COMPOSE_FILE = "docker/docker-compose.yaml"
    private static final String DOCKER_IPS_DATA_FOLDER = "/opt/Quadient/Inspire-Designer-data/ips"

    private static final IPS_SERVICE_NAME = "ips"
    private static final IPS_PORT = 30354

    private static final ICM_SERVICE_NAME = "icm"
    private static final ICM_PORT = 30353

    IpsDockerCompose() {
        super(new File(IpsDockerCompose.class.getClassLoader().getResource(DESIGNER_DOCKER_COMPOSE_FILE).toURI()))
    }

    IpsDockerCompose withExposedIcm() {
        return withExposedService(ICM_SERVICE_NAME, ICM_PORT)
    }

    IpsDockerCompose withExposedIps() {
        return withExposedService(IPS_SERVICE_NAME, IPS_PORT)
    }

    void copyDataToIpsDataFolder(String data, String fileName) {
        copyDataToIps(data, getIpsDataPathOfFile(fileName))
    }

    String copyDataFromIpsDataFolder(String fileName) {
        return getDataFromFileOnIps(getIpsDataPathOfFile(fileName))
    }

    void copFileToIpsDataFolder(Path filePath, String fileName) {
        copyFileToIps(filePath, getIpsDataPathOfFile(fileName))
    }

    String geIpsHost() {
        return getServiceHost(IPS_SERVICE_NAME, IPS_PORT)
    }

    int getIpsPort() {
        return getServicePort(IPS_SERVICE_NAME, IPS_PORT)
    }

    String getIcmHost() {
        return getServiceHost(ICM_SERVICE_NAME, ICM_PORT)
    }

    int getIcmPort() {
        return getServicePort(ICM_SERVICE_NAME, ICM_PORT)
    }

    String getIpsDataPathOfFile(String fileName) {
        return String.format("%s/%s", DOCKER_IPS_DATA_FOLDER, fileName)
    }

    Path getFileResourcePath(String fileName) {
        return Paths.get(this.class.getClassLoader().getResource("files/$fileName").toURI())
    }

    private ContainerState getIpsContainer() {
        return getContainerByServiceName(IPS_SERVICE_NAME).get()
    }

    private void copyDataToIps(String data, String ipsFilePath) {
        getIpsContainer().copyFileToContainer(
                Transferable.of(data),
                ipsFilePath
        )
    }

    private String getDataFromFileOnIps(String ipsFilePath) {
        return getIpsContainer().copyFileFromContainer(
                ipsFilePath,
                new ThrowingFunction<InputStream, String>() {
                    @Override
                    String apply(InputStream inputStream) throws Exception {
                        return new String(inputStream.readAllBytes())
                    }
                }
        )
    }

    private void copyFileToIps(Path filePath, String ipsFilePath) {
        byte[] fileData = Files.readAllBytes(filePath)
        getIpsContainer().copyFileToContainer(
                Transferable.of(fileData),
                ipsFilePath
        )
    }
}