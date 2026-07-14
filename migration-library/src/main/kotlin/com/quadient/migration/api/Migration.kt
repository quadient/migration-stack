package com.quadient.migration.api

import com.quadient.migration.api.repository.*
import com.quadient.migration.service.LocalStorage
import com.quadient.migration.service.RefCollector
import com.quadient.migration.service.ReferenceValidator
import com.quadient.migration.service.Storage
import com.quadient.migration.service.StylesValidator
import com.quadient.migration.service.deploy.CaApiClient
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.deploy.DesignerDeployClient
import com.quadient.migration.service.deploy.EvolveDeployClient
import com.quadient.migration.service.deploy.InteractiveDeployClient
import com.quadient.migration.service.deploy.utility.MetadataValidatorImpl
import com.quadient.migration.service.deploy.utility.PostProcessImpl
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.DesignerIcmDataCache
import com.quadient.migration.service.DesignerResourcePathProvider
import com.quadient.migration.service.EvolveIcmDataCache
import com.quadient.migration.service.EvolveResourcePathProvider
import com.quadient.migration.service.IcmDataCache
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.InteractiveIcmDataCache
import com.quadient.migration.service.InteractiveResourcePathProvider
import com.quadient.migration.service.PreviewProvider
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.Version
import com.quadient.migration.service.ipsclient.display
import okhttp3.OkHttpClient
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import com.quadient.migration.tools.logger
import java.util.concurrent.TimeUnit

class Migration(val config: MigConfig, val projectConfig: ProjectConfig) {
    private val logger by logger()
    private val projectName = ProjectName(projectConfig.name)

    private val designerModule = module {
        singleOf(::DesignerDeployClient) bind DeployClient::class
        singleOf(::DesignerResourcePathProvider) bind ResourcePathProvider::class
        singleOf(::DesignerIcmDataCache) bind IcmDataCache::class
        singleOf(::DesignerDocumentObjectBuilder) bind InspireDocumentObjectBuilder::class
    }

    private val evolveModule = module {
        singleOf(::EvolveDeployClient) bind DeployClient::class
        singleOf(::EvolveResourcePathProvider) bind ResourcePathProvider::class
        singleOf(::EvolveIcmDataCache) bind IcmDataCache::class
        singleOf(::InteractiveDocumentObjectBuilder) bind InspireDocumentObjectBuilder::class
        singleOf(::CaApiClient)
    }

    private val interactiveModule = module {
        singleOf(::InteractiveDeployClient) bind DeployClient::class
        singleOf(::InteractiveResourcePathProvider) bind ResourcePathProvider::class
        singleOf(::InteractiveIcmDataCache) bind IcmDataCache::class
        singleOf(::InteractiveDocumentObjectBuilder) bind InspireDocumentObjectBuilder::class
    }

    private val migrationModule = module {
        val outputModule = when(projectConfig.inspireOutput) {
            InspireOutput.Interactive -> interactiveModule
            InspireOutput.Designer -> designerModule
            InspireOutput.Evolve -> evolveModule
        }
        includes(outputModule)

        single<ProjectConfig> { projectConfig }
        single<MigConfig> { config }
        single<ProjectName> { projectName }
        single<InspireOutput> { projectConfig.inspireOutput }

        single<IpsService> { IpsService(config.inspireConfig.ipsConfig) }
        single<IcmClient> { get<IpsService>() }
        single<Storage> { get<LocalStorage>() }
        single<OkHttpClient> {
            OkHttpClient.Builder()
                .callTimeout(config.inspireConfig.evolveConfig?.callTimeoutMs ?: 10_000, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

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

        singleOf(::MetadataValidatorImpl)
        singleOf(::PostProcessImpl)
        singleOf(::ProgressReporterImpl)
        singleOf(::ConflictDetectorImpl)

        singleOf(::ReferenceValidator)
        singleOf(::StylesValidator)
        singleOf(::RefCollector)
        singleOf(::PreviewProvider)
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
    val deployClient: DeployClient by lazy { koin.get() }
    val referenceValidator: ReferenceValidator by lazy { koin.get() }
    val stylesValidator: StylesValidator by lazy { koin.get() }
    val referenceCollector: RefCollector by lazy { koin.get() }
    val pathProvider: ResourcePathProvider by lazy { koin.get() }
    val previewProvider: PreviewProvider by lazy { koin.get() }
    val storage: Storage by lazy { koin.get() }

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
                            * WARNING: Connected to unsupported IPS version $version   *
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

@JvmInline
value class ProjectName(val name: String) {
    override fun toString(): String {
        return name
    }
}
