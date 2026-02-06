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
    val repositories = mutableListOf<Repository<*>>()

    val variableRepository: Repository<Variable>
    val documentObjectRepository: Repository<DocumentObject>
    val textStyleRepository: Repository<TextStyle>
    val paragraphStyleRepository: Repository<ParagraphStyle>
    val variableStructureRepository: Repository<VariableStructure>
    val displayRuleRepository: Repository<DisplayRule>
    val imageRepository: Repository<Image>
    val attachmentRepository: Repository<Attachment>
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

        val variableRepository = VariableRepository(VariableTable, projectName)
        val documentObjectRepository = DocumentObjectRepository(DocumentObjectTable, projectName)
        val textStyleRepository = TextStyleRepository(TextStyleTable, projectName)
        val paragraphStyleRepository = ParagraphStyleRepository(ParagraphStyleTable, projectName)
        val variableStructureRepository = VariableStructureRepository(VariableStructureTable, projectName)
        val displayRuleRepository = DisplayRuleRepository(DisplayRuleTable, projectName)
        val imageRepository = ImageRepository(ImageTable, projectName)
        val attachmentRepository = AttachmentRepository(AttachmentTable, projectName)

        this.variableRepository = variableRepository
        this.documentObjectRepository = documentObjectRepository
        this.textStyleRepository = textStyleRepository
        this.paragraphStyleRepository = paragraphStyleRepository
        this.variableStructureRepository = variableStructureRepository
        this.displayRuleRepository = displayRuleRepository
        this.imageRepository = imageRepository
        this.attachmentRepository = attachmentRepository

        this.mappingRepository = MappingRepository(
            projectName,
            documentObjectRepository,
            imageRepository,
            attachmentRepository,
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
        repositories.add(attachmentRepository)

        this.referenceValidator = ReferenceValidator(
            documentObjectRepository,
            variableRepository,
            textStyleRepository,
            paragraphStyleRepository,
            variableStructureRepository,
            displayRuleRepository,
            imageRepository,
            attachmentRepository,
        )

        val inspireDocumentObjectBuilder: InspireDocumentObjectBuilder
        this.deployClient =
            if (projectConfig.inspireOutput == InspireOutput.Interactive || projectConfig.inspireOutput == InspireOutput.Evolve) {
                inspireDocumentObjectBuilder = InteractiveDocumentObjectBuilder(
                    documentObjectRepository,
                    textStyleRepository,
                    paragraphStyleRepository,
                    variableRepository,
                    variableStructureRepository,
                    displayRuleRepository,
                    imageRepository,
                    attachmentRepository,
                    projectConfig,
                    ipsService,
                )

                InteractiveDeployClient(
                    documentObjectRepository,
                    imageRepository,
                    attachmentRepository,
                    statusTrackingRepository,
                    textStyleRepository,
                    paragraphStyleRepository,
                    inspireDocumentObjectBuilder,
                    ipsService,
                    storage,
                    projectConfig
                )
            } else {
                inspireDocumentObjectBuilder  = DesignerDocumentObjectBuilder(
                    documentObjectRepository,
                    textStyleRepository,
                    paragraphStyleRepository,
                    variableRepository,
                    variableStructureRepository,
                    displayRuleRepository,
                    imageRepository,
                    attachmentRepository,
                    projectConfig,
                    ipsService,
                )

                DesignerDeployClient(
                    documentObjectRepository,
                    imageRepository,
                    attachmentRepository,
                    statusTrackingRepository,
                    textStyleRepository,
                    paragraphStyleRepository,
                    inspireDocumentObjectBuilder,
                    ipsService,
                    storage,
                )
            }

        this.stylesValidator = StylesValidator(
            documentObjectRepository,
            textStyleRepository,
            paragraphStyleRepository,
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