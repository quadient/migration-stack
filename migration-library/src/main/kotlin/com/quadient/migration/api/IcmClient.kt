package com.quadient.migration.api

import com.quadient.migration.service.ipsclient.IpsClientException
import com.quadient.migration.shared.IcmFileMetadata
import com.quadient.migration.shared.MetadataValue
import java.io.IOException

interface IcmClient {
    /**
     * Uploads a file to the connected ICM
     * @param path Path to the file. Must start with "icm://"
     * @param data ByteArray of the content
     * @throws IllegalArgumentException if the path does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IOException
     */
    fun upload(path: String, data: ByteArray)

    /**
     * Downloads a file from the connected ICM
     * @param path Path to the file. Must start with "icm://"
     * @return ByteArray containing the file content
     * @throws IllegalArgumentException if the path does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IOException
     */
    fun download(path: String): ByteArray

    /**
     * Approves files in the connected ICM
     * @param paths List of paths to the files. Each path must start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     */
    fun approveFiles(paths: List<String>)

    /**
     * Checks whether given file exists in ICM
     * @return true if file exists, false otherwise
     * @param path Path to file in ICM. Path must start with "icm://"
     * @throws IllegalArgumentException if the path does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IllegalStateException if unexpected result is returned from IPS
     * @throws IOException
     */
    fun fileExists(path: String): Boolean

    /**
     * Checks whether each path in the list corresponds with an existing file in ICM
     * @return List of results, where each element corresponds to the input path in the
     * same order as they were provided.
     * @param paths List of paths to the files Each path must start with "icm://"
     * @throws IllegalArgumentException if any of the paths does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IllegalStateException if unexpected result is returned from IPS
     * @throws IOException
     */
    fun filesExist(paths: List<String>): List<Boolean>

    /**
     * Hard delete a file in the connected ICM
     * @return true if file was deleted, false otherwise
     * @param path Path to file in ICM. Path must start with "icm://"
     * @throws IllegalArgumentException if the path does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IllegalStateException if unexpected result is returned from IPS
     * @throws IOException
     */
    fun delete(path: String): Boolean

    /**
     * Hard delete all the provided files in the connected ICM
     * @return List of results, where each element corresponds to the input path in the
     * same order as they were provided. True if file was deleted, false otherwise.
     * @param paths List of paths to the files Each path must start with "icm://"
     * @throws IllegalArgumentException if any of the paths does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IllegalStateException if unexpected result is returned from IPS
     * @throws IOException
     */
    fun delete(paths: List<String>): List<Boolean>

    /**
     * Read metadata of a single ICM file
     * @return Metadata of the ICM file
     * @param paths List of paths to the files Each path must start with "icm://"
     * @throws IllegalArgumentException if any of the paths does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IOException
     */
    fun readMetadata(path: String): IcmFileMetadata

    /**
     * Read metadata of all the provided files in the connected ICM
     * @return List of results, where each element corresponds to the input path in the
     * same order as they were provided.
     * @param paths List of paths to the files Each path must start with "icm://"
     * @throws IllegalArgumentException if any of the paths does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IOException
     */
    fun readMetadata(paths: List<String>): List<IcmFileMetadata>

    /**
     * Write metadata to all the provided files in the connected ICM
     * @param metadata List of IcmFileMetadata objects containing path and metadata to write
     * @throws IllegalArgumentException if any of the paths does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IOException
     */
    fun writeMetadata(metadata: List<IcmFileMetadata>)

    /**
     * Write metadata to a single file in the connected ICM
     * @param path Path to the file. Must start with "icm://"
     * @param metadata Map of metadata key-value pairs to write
     * @throws IllegalArgumentException if the path does not start with "icm://"
     * @throws IpsClientException if failure occurs when working with IPS
     * @throws IOException
     */
    fun writeMetadata(path: String, metadata: Map<String, MetadataValue>)
}