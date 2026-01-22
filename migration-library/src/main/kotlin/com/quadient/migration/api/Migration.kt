package com.quadient.migration.api

import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.repository.*
import com.quadient.migration.data.*
import com.quadient.migration.persistence.repository.*
import com.quadient.migration.persistence.table.*
import com.quadient.migration.service.LocalStorage
import com.quadient.migration.service.ReferenceValidator
import com.quadient.migration.service.Storage
import com.quadient.migration.service.StylesValidator
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.deploy.DesignerDeployClient
import com.quadient.migration.service.deploy.InteractiveDeployClient
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.Version
import com.quadient.migration.service.ipsclient.display
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
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
    val mappingRepository: MappingRepository

    val icmClient: IcmClient = ipsService

    val deployClient: DeployClient

    val referenceValidator: ReferenceValidator
    val stylesValidator: StylesValidator
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

        Flyway.configure()
            .dataSource(config.dbConfig.connectionString(), config.dbConfig.user, config.dbConfig.password)
            .locations("classpath:com/quadient/migration/persistence/upgrade")
            .load()
            .migrate()

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

        this.mappingRepository = MappingRepository(
            projectName,
            documentObjectRepository,
            imageRepository,
            textStyleRepository,
            paragraphStyleRepository,
            variableRepository,
            variableStructureRepository
        )

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

        val inspireDocumentObjectBuilder: InspireDocumentObjectBuilder
        this.deployClient =
            if (projectConfig.inspireOutput == InspireOutput.Interactive || projectConfig.inspireOutput == InspireOutput.Evolve) {
                inspireDocumentObjectBuilder = InteractiveDocumentObjectBuilder(
                    documentObjectInternalRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    variableInternalRepository,
                    variableStructureInternalRepository,
                    displayRuleInternalRepository,
                    imageInternalRepository,
                    projectConfig,
                    ipsService,
                )

                InteractiveDeployClient(
                    documentObjectInternalRepository,
                    imageInternalRepository,
                    statusTrackingRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    inspireDocumentObjectBuilder,
                    ipsService,
                    storage,
                    projectConfig
                )
            } else {
                inspireDocumentObjectBuilder  = DesignerDocumentObjectBuilder(
                    documentObjectInternalRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    variableInternalRepository,
                    variableStructureInternalRepository,
                    displayRuleInternalRepository,
                    imageInternalRepository,
                    projectConfig,
                    ipsService,
                )

                DesignerDeployClient(
                    documentObjectInternalRepository,
                    imageInternalRepository,
                    statusTrackingRepository,
                    textStyleInternalRepository,
                    paragraphStyleInternalRepository,
                    inspireDocumentObjectBuilder,
                    ipsService,
                    storage,
                )
            }

        this.stylesValidator = StylesValidator(
            documentObjectInternalRepository,
            textStyleInternalRepository,
            paragraphStyleInternalRepository,
            inspireDocumentObjectBuilder,
            deployClient,
            ipsService
        )

        logger.debug("Setting up shutdown hook for IPS service")
        Runtime.getRuntime().addShutdownHook(Thread {
                val version = ipsService.client.version
                if (version != null) {
                    if (version.isSupportedMajorVersion() && !version.isSupportedVersion()) {
                        logger.warn("""
                            
                            ************************************************************
                            *                                                          *
                            * WARNING: Connected to unsupported IPS version $version *
                            *                                                          *
                            ************************************************************
                            """.trimIndent()
                        )
                        logger.warn(
                            "Supported IPS versions are: ${
                                Version.SUPPORTED_VERSION_RANGES.joinToString(
                                    prefix = "[",
                                    postfix = "]"
                                ) { it.display() }
                            }"
                        )
                    }
                }

                logger.trace("Shutdown hook triggered, closing IPS service connections")
                ipsService.close()
            }
        )

        logger.debug("Migration initialized")
    }
}