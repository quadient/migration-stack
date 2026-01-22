package com.quadient.migration.service.ipsclient

data class Version(val major: Int, val minor: Int, val patch: Int, val revision: Int) : Comparable<Version> {
    companion object {
        val SUPPORTED_MAJOR_VERSION_RANGE = listOf(Version(17, 0, 600, 0)..Version(17, 0, 999, 9))
        val SUPPORTED_VERSION_RANGES = listOf(Version(17, 0, 638, 0)..Version(17, 0, 999, 9))

        fun parse(versionString: String): ParseResult {
            val parts = versionString.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull()
                ?: return ParseResult.Failure("Invalid major version in '$versionString'")
            val minor = parts.getOrNull(1)?.toIntOrNull()
                ?: return ParseResult.Failure("Invalid minor version in '$versionString'")
            val patch = parts.getOrNull(2)?.toIntOrNull()
                ?: return ParseResult.Failure("Invalid patch version in '$versionString'")
            val revision = parts.getOrNull(3)?.toIntOrNull()
                ?: return ParseResult.Failure("Invalid revision version in '$versionString'")
            return ParseResult.Success(Version(major, minor, patch, revision))
        }
    }

    override fun compareTo(other: Version): Int {
        if (this.major != other.major) {
            return this.major - other.major
        }
        if (this.minor != other.minor) {
            return this.minor - other.minor
        }
        if (this.patch != other.patch) {
            return this.patch - other.patch
        }
        return this.revision - other.revision
    }

    fun isSupportedMajorVersion(): Boolean {
        return SUPPORTED_MAJOR_VERSION_RANGE.any { range -> this in range }
    }

    fun isSupportedVersion(): Boolean {
        return SUPPORTED_VERSION_RANGES.any { range -> this in range }
    }

    override fun toString(): String {
        return "$major.$minor.$patch.$revision"
    }

    sealed class ParseResult {
        data class Success(val version: Version) : ParseResult()
        data class Failure(val reason: String) : ParseResult()
    }
}

fun ClosedRange<Version>.display(): String {
    return "${this.start} and higher"
}