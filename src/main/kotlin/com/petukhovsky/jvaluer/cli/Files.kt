package com.petukhovsky.jvaluer.cli

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

inline fun <reified T: Any> readJSON(string: String): T = readJSON<T>(pathJSON(string)!!)
fun pathJSON(string: String, ifNull: () -> Unit = { println("File $string not found.") }): Path? {
    val path1 = Paths.get(string + ".json")
    if (Files.exists(path1)) return path1
    val path2 = Paths.get(string)
    return if (Files.exists(path2)) path2 else {
        ifNull()
        null
    }
}