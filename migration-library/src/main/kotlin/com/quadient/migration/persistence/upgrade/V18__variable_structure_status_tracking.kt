package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * Variable structures did not have status tracking until they became deployable (as data definitions).
 * Creates an initial Active status record for every existing variable structure that has none,
 * so they are picked up by the deployment.
 */
class V18__variable_structure_status_tracking : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                INSERT INTO status_tracking (id, resource_type, project_name, status_events)
                SELECT vs.id, 'VariableStructure', vs.project_name,
                       jsonb_build_array(jsonb_build_object(
                           'type', 'Active',
                           'timestamp', to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
                       ))
                FROM variable_structure vs
                ON CONFLICT (id, resource_type, project_name) DO NOTHING
                """.trimIndent()
            )
        }
    }
}
