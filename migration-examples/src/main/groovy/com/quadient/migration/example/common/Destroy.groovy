package com.quadient.migration.example.common

import static com.quadient.migration.example.common.util.InitMigration.initMigration

// **********************************************
//               !!!CAREFUL!!!
//      THIS WILL DELETE ALL PROJECT DATA
// **********************************************

println "!!!CAREFUL!!!"
println "Are you really sure you want to delete all project data? (y/n)"
def answer = System.in.newReader().readLine()

if (answer != "y") {
    println("Not destroying anything")
    System.exit(0)
}

println("Destroying all project data...")
def migration = initMigration(this.binding.variables["args"])

migration.repositories.forEach { it.deleteAll() }
migration.storage.deleteAll()