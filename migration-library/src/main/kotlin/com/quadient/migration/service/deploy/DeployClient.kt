package com.quadient.migration.service.deploy

import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.ImageType
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.InvalidPathException

data class DocObjectWithRef(val obj: DocumentObjectModel, val documentObjectRefs: Set<String>)

abstract class DeployClient(
    protected val documentObjectRepository: DocumentObjectInternalRepository,
    protected val imageRepository: ImageInternalRepository,
    protected val documentObjectBuilder: InspireDocumentObjectBuilder,
    protected val ipsService: IpsService,
    protected val storage: Storage
) {
    protected val logger = LoggerFactory.getLogger(this::class.java)!!

    abstract fun deployDocumentObjects(documentObjectIds: List<String>, skipDependencies: Boolean): DeploymentResult
    abstract fun deployDocumentObjects(): DeploymentResult

    abstract fun shouldIncludeDependency(documentObject: DocumentObjectModel): Boolean
    abstract fun shouldIncludeImage(documentObject: DocumentObjectModel): Boolean

    fun deployDocumentObjects(documentObjectIds: List<String>) = deployDocumentObjects(documentObjectIds, false)

    fun deployStyles() {
        val outputPath = documentObjectBuilder.getStyleDefinitionPath()
        val xml2wfdResult = ipsService.xml2wfd(documentObjectBuilder.buildStyles(), outputPath)
        if (xml2wfdResult == OperationResult.Success) {
            logger.debug("Deployment of $outputPath is successful.")
        } else {
            logger.error("Failed to deploy $outputPath.")
            ipsService.close()
            return
        }

        val approvalResult = ipsService.setProductionApprovalState(listOf(outputPath))
        when (approvalResult) {
            is OperationResult.Failure -> logger.error("Failed to set production approval state to $outputPath.")
            OperationResult.Success -> logger.debug("Setting of production approval state to $outputPath is successful.")
        }

        ipsService.close()
    }

    fun deployOrder(documentObjects: List<DocumentObjectModel>): List<DocumentObjectModel> {
        val documentObjectIds = documentObjects.map { it.id }

        val deployOrder = mutableListOf<DocumentObjectModel>()

        var toCheck = documentObjects.map {
            DocObjectWithRef(
                it, it.collectRefs().filterIsInstance<DocumentObjectModelRef>().map { it -> it.id }.toSet()
            )
        }
        val deployed = mutableSetOf<String>()

        var lastSize = toCheck.size
        while (deployed.size < documentObjects.size) {
            val (canDeploy, cantDeploy) = toCheck.partition { docObj ->
                docObj.documentObjectRefs.all {
                    deployed.contains(it) || !documentObjectIds.contains(it)
                }
            }

            if (cantDeploy.isEmpty()) {
                for (item in canDeploy) {
                    deployOrder.add(item.obj)
                }
                return deployOrder
            }

            if (lastSize == cantDeploy.size) {
                logger.error(
                    "Cannot determine deploy order. Either circular reference or some references are missing. Deployed: ${
                        deployed.joinToString(
                            separator = ",", prefix = "'[", postfix = "]'"
                        )
                    }, Can deploy: ${
                        canDeploy.joinToString(
                            separator = ",", prefix = "'[", postfix = "]'"
                        )
                    }, Cannot deploy: ${cantDeploy.joinToString(separator = ",", prefix = "'[", postfix = "]'")}"
                )
                throw RuntimeException("Cannot determine deploy order. Either circular reference or some references are missing.")
            }

            for (item in canDeploy) {
                deployOrder.add(item.obj)
                deployed.add(item.obj.id)
            }

            lastSize = cantDeploy.size
            toCheck = cantDeploy
        }

        return deployOrder
    }

    protected fun deployImages(documentObjects: List<DocumentObjectModel>): DeploymentResult {
        val deploymentResult = DeploymentResult()

        val uniqueImageRefs = documentObjects.flatMap {
            try {
                it.getAllDocumentObjectImageRefs()
            } catch (e: IllegalStateException) {
                deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
                emptyList()
            }
        }.distinct()

        uniqueImageRefs.forEach { imageRef ->
            val imageModel = imageRepository.findModel(imageRef.id)
            if (imageModel == null) {
                val message = "Image '${imageRef.id}' not found."
                logger.error(message)
                deploymentResult.errors.add(DeploymentError(imageRef.id, message))
                return@forEach
            }

            if (imageModel.imageType == ImageType.Unknown) {
                val message = "Skipping deployment of image '${imageModel.nameOrId()}' due to unknown image type."
                logger.warn(message)
                deploymentResult.warnings.add(DeploymentWarning(imageRef.id, message))
                return@forEach
            }

            if (imageModel.sourcePath.isNullOrBlank()) {
                val message = "Skipping deployment of image '${imageModel.nameOrId()}' due to missing source path."
                logger.warn(message)
                deploymentResult.warnings.add(DeploymentWarning(imageRef.id, message))
                return@forEach
            }

            logger.debug("Starting deployment of image '${imageModel.nameOrId()}'.")
            val readResult = readStorageSafely(imageModel.sourcePath)
            if (readResult is ReadResult.Error) {
                val message = "Error while reading image source data: ${readResult.errorMessage}."
                logger.error(message)
                deploymentResult.errors.add(DeploymentError(imageRef.id, message))
                return@forEach
            }

            val imageData = (readResult as ReadResult.Success).result

            logger.trace("Loaded image data of size ${imageData.size} from storage.")

            val icmImagePath = documentObjectBuilder.getImagePath(imageModel)
            val uploadResult = ipsService.tryUpload(icmImagePath, imageData)
            if (uploadResult is OperationResult.Failure) {
                deploymentResult.errors.add(DeploymentError(imageRef.id, uploadResult.message))
                return@forEach
            }

            logger.debug("Deployment of image '${imageModel.nameOrId()}' to '${icmImagePath}' is successful.")
            deploymentResult.deployed.add(DeploymentInfo(imageRef.id, ResourceType.Image, icmImagePath))
        }

        return deploymentResult
    }

    fun DocumentObjectModel.findDependencies(): List<DocumentObjectModel> {
        val dependencies = mutableListOf<DocumentObjectModel>()
        this.collectRefs().forEach { ref ->
            when (ref) {
                is DisplayRuleModelRef, is TextStyleModelRef, is ParagraphStyleModelRef, is VariableModelRef -> {}
                is ImageModelRef -> {}
                is DocumentObjectModelRef -> {
                    val model = documentObjectRepository.findModelOrFail(ref.id)
                    if (shouldIncludeDependency(model)) {
                        dependencies.add(model)
                        dependencies.addAll(model.findDependencies())
                    }
                }
            }
        }
        return dependencies
    }

    private fun DocumentObjectModel.getAllDocumentObjectImageRefs(): List<ImageModelRef> {
        return this.collectRefs().flatMap { ref ->
            when (ref) {
                is DisplayRuleModelRef, is TextStyleModelRef, is ParagraphStyleModelRef, is VariableModelRef -> emptyList()
                is ImageModelRef -> listOf(ref)
                is DocumentObjectModelRef -> {
                    val model = documentObjectRepository.findModel(ref.id)
                        ?: error("Unable to collect image references because inner document object '${ref.id}' was not found.")

                    if (shouldIncludeImage(model)) {
                        model.getAllDocumentObjectImageRefs()
                    } else {
                        emptyList()
                    }
                }
            }
        }
    }

    private sealed interface ReadResult {
        data class Success(val result: ByteArray) : ReadResult
        data class Error(val errorMessage: String) : ReadResult
    }

    private fun readStorageSafely(path: String): ReadResult {
        try {
            val byteArray = storage.read(path)
            return ReadResult.Success(byteArray)
        } catch (_: InvalidPathException) {
            return ReadResult.Error("File path '$path' is invalid.")
        } catch (_: SecurityException) {
            return ReadResult.Error("Access to file '$path' is denied.")
        } catch (_: FileNotFoundException) {
            return ReadResult.Error("File '$path' not found.")
        } catch (_: IOException) {
            return ReadResult.Error("I/O error occurred.")
        } catch (_: Exception) {
            return ReadResult.Error("Unexpected error.")
        }
    }
}