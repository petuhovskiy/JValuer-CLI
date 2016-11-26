package com.petukhovsky.jvaluer.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun getNullablePath(string: String?): Path? {
    return Paths.get(string ?: return null)
}

inline fun <reified T: Any> readJSON(path: Path): T {
    return Files.newInputStream(path).use {
        objectMapper.readValue<T>(it)
    }
}