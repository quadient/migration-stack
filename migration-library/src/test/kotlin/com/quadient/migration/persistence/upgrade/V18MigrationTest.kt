package com.quadient.migration.persistence.upgrade

import com.quadient.migration.Postgres.Companion.POSTGRES_CONTAINER
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectName
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.service.deploy.utility.ResourceType
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager

/**
 * Integration test for V18 migration that backfills an Active status tracking record
 * for every existing variable structure.
 */
class V18MigrationTest {

    @Test
    fun `V18 migration creates active status for existing variable structures`() {
        PostgreSQLContainer(POSTGRES_CONTAINER).use { postgres ->
            postgres.start()

            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )

            migrate(postgres, "17")

            val deployedEvent =
                """[{"type":"Deployed","deploymentId":"00000000-0000-0000-0000-000000000001","timestamp":"2026-01-01T00:00:00Z","output":"Interactive","icmPath":"icm://x.jdd","data":{}}]"""
            connection(postgres).use { conn ->
                insertVariableStructure(conn, "vs1", "test")
                insertVariableStructure(conn, "vs2", "other")
                insertStatus(conn, "vs1", "DocumentObject", "test", deployedEvent)
            }

            migrate(postgres, "18")

            val testRepo = StatusTrackingRepository(ProjectName("test"))
            val otherRepo = StatusTrackingRepository(ProjectName("other"))

            assertInstanceOf(
                Active::class.java,
                testRepo.findLastEventRelevantToOutput("vs1", ResourceType.VariableStructure, InspireOutput.Interactive)
            )
            assertInstanceOf(
                Active::class.java,
                otherRepo.findLastEventRelevantToOutput("vs2", ResourceType.VariableStructure, InspireOutput.Interactive)
            )
            assertNull(otherRepo.findLastEventRelevantToOutput("vs1", ResourceType.VariableStructure, InspireOutput.Interactive))

            // status of a different resource type with the same id is left untouched
            assertInstanceOf(
                Deployed::class.java,
                testRepo.findLastEventRelevantToOutput("vs1", ResourceType.DocumentObject, InspireOutput.Interactive)
            )

            connection(postgres).use { conn ->
                val rs = conn.createStatement()
                    .executeQuery("SELECT count(*) FROM status_tracking WHERE resource_type = 'VariableStructure'")
                rs.next()
                assertEquals(2, rs.getInt(1))
            }
        }
    }

    private fun migrate(postgres: PostgreSQLContainer, target: String) {
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:com/quadient/migration/persistence/upgrade")
            .target(target)
            .load()
            .migrate()
    }

    private fun insertVariableStructure(conn: Connection, id: String, projectName: String) {
        conn.prepareStatement(
            """
            INSERT INTO variable_structure (id, project_name, name, origin_locations, custom_fields, last_updated, created, structure)
            VALUES (?, ?, ?, '{}', '{}', NOW(), NOW(), '{}')
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, projectName)
            setString(3, id)
        }.executeUpdate()
    }

    private fun insertStatus(conn: Connection, id: String, type: String, projectName: String, events: String) {
        conn.prepareStatement(
            """
            INSERT INTO status_tracking (id, resource_type, project_name, status_events)
            VALUES (?, ?, ?, ?::jsonb)
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, type)
            setString(3, projectName)
            setString(4, events)
        }.executeUpdate()
    }

    private fun connection(postgres: PostgreSQLContainer): Connection =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
}
