package com.quadient.migration.example.common.util

import java.nio.file.Path
import java.nio.file.Paths

static Path dataDirPath(Binding binding, String... paths) {
    if (binding != null && binding.variables.containsKey("DATA_DIR")) {
        return Paths.get(binding.DATA_DIR, *paths)
    }

    return Paths.get(*paths)
}