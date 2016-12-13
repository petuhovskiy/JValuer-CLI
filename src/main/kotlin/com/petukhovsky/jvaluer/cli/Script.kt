package com.petukhovsky.jvaluer.cli

import com.fasterxml.jackson.annotation.JsonIgnore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class Script(
        @JsonIgnore var locationPath: Path? = null
) {
    open fun execute() {}

    fun resolve(string: String): Path {
        return locationPath?.resolveSibling(string) ?: Paths.get(string)
    }
}

inline fun <reified T: Script> readScript(path: Path): T = readJSON<T>(path).apply { locationPath = path }
inline fun <reified T: Script> readScript(string: String): T = readScript<T>(Paths.get(string))