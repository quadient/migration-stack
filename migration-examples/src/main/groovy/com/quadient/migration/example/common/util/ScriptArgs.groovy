package com.quadient.migration.example.common.util

static Optional<String> getValueOfArg(String arg, List<String> args) {
    def argIndex = args.findIndexOf { it == arg }
    if (argIndex > -1) {
        def argValue = args[argIndex + 1]
        if (argValue == null) {
            println("Value for arg '$arg' is not specified.")
            return Optional.empty()
        } else {
            return Optional.of(argValue)
        }
    }
    return Optional.empty()
}

