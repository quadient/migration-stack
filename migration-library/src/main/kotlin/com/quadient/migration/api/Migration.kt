package com.quadient.migration.api

import com.quadient.migration.api.repository.*
import com.quadient.migration.service.LocalStorage
import com.quadient.migration.service.RefCollector
import com.quadient.migration.service.ReferenceValidator
import com.quadient.migration.service.Storage
import com.quadient.migration.service.StylesValidator
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.deploy.DesignerDeployClient
import com.quadient.migration.service.deploy.InteractiveDeployClient
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.MetadataValidatorImpl
import com.quadient.migration.service.deploy.utility.PostProcessImpl
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.Version
import com.quadient.migration.service.ipsclient.display
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.slf4j.LoggerFactory

@JvmInline
value class ProjectName(val name: String) {
    override fun toString(): String {
        return name
    }
}

class Migration(val config: MigConfig, val projectConfig: ProjectConfig) {
    private val logger = LoggerFactory.getLogger(Migration::class.java)
    private val projectName = ProjectName(projectConfig.name)

    private val migrationModule = module {
        single<ProjectConfig> { projectConfig }
        single<MigConfig> { config }
        single<ProjectName> { projectName }

        single<IpsService> { IpsService(config.inspireConfig.ipsConfig) }
        single<IcmClient> { get<IpsService>() }
        single<Storage> { get<LocalStorage>() }

        singleOf(::VariableRepository)
        singleOf(::DocumentObjectRepository)
        singleOf(::TextStyleRepository)
        singleOf(::ParagraphStyleRepository)
        singleOf(::VariableStructureRepository)
        singleOf(::DisplayRuleRepository)
        singleOf(::ImageRepository)
        singleOf(::AttachmentRepository)
        singleOf(::StatusTrackingRepository)
        singleOf(::MappingRepository)

        singleOf(::LocalStorage)

        singleOf(::InteractiveDocumentObjectBuilder)
        singleOf(::DesignerDocumentObjectBuilder)

        singleOf(::InteractiveDeployClient)
        singleOf(::DesignerDeployClient)

        singleOf(::MetadataValidatorImpl)
        singleOf(::PostProcessImpl)

        singleOf(::ReferenceValidator)
        singleOf(::StylesValidator)
        singleOf(::RefCollector)
    }

    private val koinApp: KoinApplication = koinApplication {
        modules(migrationModule)
    }

    private val koin = koinApp.koin

    val repositories = mutableListOf<Repository<*>>()

    val variableRepository: VariableRepository by lazy { koin.get() }
    val documentObjectRepository: DocumentObjectRepository by lazy { koin.get() }
    val textStyleRepository: TextStyleRepository by lazy { koin.get() }
    val paragraphStyleRepository: ParagraphStyleRepository by lazy { koin.get() }
    val variableStructureRepository: VariableStructureRepository by lazy { koin.get() }
    val displayRuleRepository: DisplayRuleRepository by lazy { koin.get() }
    val imageRepository: ImageRepository by lazy { koin.get() }
    val attachmentRepository: AttachmentRepository by lazy { koin.get() }
    val statusTrackingRepository: StatusTrackingRepository by lazy { koin.get() }
    val mappingRepository: MappingRepository by lazy { koin.get() }

    val icmClient: IcmClient by lazy { koin.get() }
    val deployClient: DeployClient by lazy {
        if (projectConfig.inspireOutput in listOf(InspireOutput.Interactive, InspireOutput.Evolve)) {
            koin.get<InteractiveDeployClient>()
        } else {
            koin.get<DesignerDeployClient>()
        }
    }
    val referenceValidator: ReferenceValidator by lazy { koin.get() }
    val stylesValidator: StylesValidator by lazy { koin.get() }
    val referenceCollector: RefCollector by lazy { koin.get() }
    val storage: Storage by lazy { koin.get<LocalStorage>() }

    private val ipsService: IpsService by lazy { koin.get() }

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

        repositories.add(variableRepository)
        repositories.add(documentObjectRepository)
        repositories.add(textStyleRepository)
        repositories.add(paragraphStyleRepository)
        repositories.add(variableStructureRepository)
        repositories.add(displayRuleRepository)
        repositories.add(imageRepository)
        repositories.add(attachmentRepository)

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