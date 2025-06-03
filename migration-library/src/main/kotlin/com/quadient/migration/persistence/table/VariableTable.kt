package com.quadient.migration.persistence.table

object VariableTable : MigrationObjectTable("variable") {
    val dataType = varchar("data_type", 50)
    val defaultValue = varchar("default_value", 255).nullable()
}
