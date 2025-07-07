package com.quadient.migration.api

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.data.VariableModel
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.persistence.table.StatusTrackingTable
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.persistence.table.VariableTable
import com.quadient.migration.service.LocalStorage
import com.quadient.migration.service.ReferenceValidator
import com.quadient.migration.service.Storage
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.deploy.DesignerDeployClient
import com.quadient.migration.service.deploy.InteractiveDeployClient
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

class Migration(public val config: MigConfig, public val projectConfig: ProjectConfig) {
    private val logger = LoggerFactory.getLogger(Migration::class.java)!!

    private val projectName = projectConfig.name
    private val ipsConfig = config.inspireConfig.ipsConfig
    private val ipsService = IpsService(ipsConfig)
    val repositories = mutableListOf<Repository<*, *>>()

    val variableRepository: Repository<Variable, VariableModel>
    val documentObjectRepository: Repository<DocumentObject, DocumentObjectModel>
    val textStyleRepository: Repository<TextStyle, TextStyleModel>
    val paragraphStyleRepository: Repository<ParagraphStyle, ParagraphStyleModel>
    val variableStructureRepository: Repository<VariableStructure, VariableStructureModel>
    val displayRuleRepository: Repository<DisplayRule, DisplayRuleModel>
    val imageRepository: Repository<Image, ImageModel>
    val statusTrackingRepository = StatusTrackingRepository(projectName)

    val icmClient: IcmClient = ipsService
    val ipsClient = ipsService.client

    val deployClient: DeployClient

    val referenceValidator: ReferenceValidator
    val storage: Storage by lazy {
        require(config.storageRoot != null) { "'storageRoot' must be configured in order to use the storage" }
        LocalStorage(config.storageRoot, projectName)
    }

    init {
        Database.connect(
            url = config.dbConfig.connectionString(),
            driver = "org.postgresql.Driver",
            user = config.dbConfig.user,
            password = config.dbConfig.password
        )

        transaction {
            SchemaUtils.create(
                VariableTable,
                DocumentObjectTable,
                TextStyleTable,
                ParagraphStyleTable,
                VariableStructureTable,
                DisplayRuleTable,
                ImageTable,
                StatusTrackingTable
            )
        }

        val documentObjectInternalRepository = DocumentObjectInternalRepository(DocumentObjectTable, projectName)
        val imageInternalRepository = ImageInternalRepository(ImageTable, projectName)
        val displayRuleInternalRepository = DisplayRuleInternalRepository(DisplayRuleTable, projectName)
        val variableInternalRepository = VariableInternalRepository(VariableTable, projectName)
        val variableStructureInternalRepository =
            VariableStructureInternalRepository(VariableStructureTable, projectName)
        val textStyleInternalRepository = TextStyleInternalRepository(TextStyleTable, projectName)
        val paragraphStyleInternalRepository = ParagraphStyleInternalRepository(ParagraphStyleTable, projectName)

        val variableRepository = VariableRepository(variableInternalRepository)
        val documentObjectRepository = DocumentObjectRepository(documentObjectInternalRepository)
        val textStyleRepository = TextStyleRepository(textStyleInternalRepository)
        val paragraphStyleRepository = ParagraphStyleRepository(paragraphStyleInternalRepository)
        val variableStructureRepository = VariableStructureRepository(variableStructureInternalRepository)
        val displayRuleRepository = DisplayRuleRepository(displayRuleInternalRepository)
        val imageRepository = ImageRepository(imageInternalRepository)

        this.variableRepository = variableRepository
        this.documentObjectRepository = documentObjectRepository
        this.textStyleRepository = textStyleRepository
        this.paragraphStyleRepository = paragraphStyleRepository
        this.variableStructureRepository = variableStructureRepository
        this.displayRuleRepository = displayRuleRepository
        this.imageRepository = imageRepository

        repositories.add(variableRepository)
        repositories.add(documentObjectRepository)
        repositories.add(textStyleRepository)
        repositories.add(paragraphStyleRepository)
        repositories.add(variableStructureRepository)
        repositories.add(displayRuleRepository)
        repositories.add(imageRepository)

        this.referenceValidator = ReferenceValidator(
            documentObjectInternalRepository,
            variableInternalRepository,
            textStyleInternalRepository,
            paragraphStyleInternalRepository,
            variableStructureInternalRepository,
            displayRuleInternalRepository,
            imageInternalRepository,
        )

        this.deployClient =
            if (projectConfig.inspireOutput == InspireOutput.Interactive || projectConfig.inspireOutput == InspireOutput.Evolve) {
                val interactiveDocumentObjectBuilder = InteractiveDocumentObjectBuilder(
                    documentObjectInternalRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    variableInternalRepository,
                    variableStructureInternalRepository,
                    displayRuleInternalRepository,
                    imageInternalRepository,
                    projectConfig
                )

                InteractiveDeployClient(
                    documentObjectInternalRepository,
                    imageInternalRepository,
                    statusTrackingRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    interactiveDocumentObjectBuilder,
                    ipsService,
                    storage,
                    projectConfig
                )
            } else {
                val designerDocumentObjectBuilder = DesignerDocumentObjectBuilder(
                    documentObjectInternalRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    variableInternalRepository,
                    variableStructureInternalRepository,
                    displayRuleInternalRepository,
                    imageInternalRepository,
                    projectConfig
                )

                DesignerDeployClient(
                    documentObjectInternalRepository,
                    imageInternalRepository,
                    statusTrackingRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    designerDocumentObjectBuilder,
                    ipsService,
                    storage,
                )
            }

        logger.debug("Migration initialized")
    }
}