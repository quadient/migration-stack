package com.quadient.migration

import com.quadient.migration.api.DbConfig
import com.quadient.migration.api.Migration
import com.quadient.migration.tools.aMigConfig
import com.quadient.migration.tools.aProjectConfig
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Target(AnnotationTarget.CLASS)
@ExtendWith(Postgres.PostgresExtension::class)
annotation class Postgres {

    companion object {
        const val POSTGRES_CONTAINER = "postgres:16-alpine"
    }

    @Testcontainers
    class PostgresExtension : BeforeAllCallback, AfterAllCallback, AfterEachCallback {
        @Container
        private val postgres = PostgreSQLContainer(POSTGRES_CONTAINER)
        private lateinit var mig: Migration

        override fun beforeAll(p0: ExtensionContext) {
            postgres.start()
            mig = Migration(
                aMigConfig(
                    dbConfig = DbConfig(
                        host = postgres.host,
                        port = postgres.firstMappedPort,
                        dbName = postgres.databaseName,
                        user = postgres.username,
                        password = postgres.password,
                    )
                ),
                aProjectConfig()
            )
        }

        override fun afterAll(p0: ExtensionContext) {
            postgres.stop()
        }

        override fun afterEach(p0: ExtensionContext) {
            mig.repositories.forEach { it.deleteAll() }
        }
    }
}
