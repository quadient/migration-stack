package com.quadient.migration.example.common.util

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

static Optional<String> getValueOfArg(String arg, List<String> args) {
    def argIndex = args.findIndexOf { it == arg }
    if (argIndex > -1) {
        def argValue = args[argIndex + 1]
        if (argValue == null) {
            log.debug "Value for arg '$arg' is not specified."
            return Optional.empty()
        } else {
            return Optional.of(argValue)
        }
    }
    return Optional.empty()
}

