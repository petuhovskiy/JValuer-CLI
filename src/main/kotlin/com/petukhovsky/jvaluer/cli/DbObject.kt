package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.cli.objectMapper
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

private val prefix = "db/"

class DbObject<T>(path: String, val c: Class<T>) {

    val bak = configDir.resolve("$path.bak")
    val json = configDir.resolve("$path.json")

    fun exists() = Files.exists(bak) || Files.exists(json)

    fun get(): T? {
        if (!exists()) return null
        if (Files.exists(bak)) {
            Files.move(bak, json, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return get()
        }
        assert(Files.exists(json), {"Unexpected missing file: ${json.toAbsolutePath()}"})
        return objectMapper.readValue(json.toFile(), c)
    }

    fun save(value: T) {
        Files.createDirectories(json.parent)
        if (Files.exists(json)) Files.move(json, bak, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Files.newOutputStream(json).use { objectMapper.writeValue(it, value) }
        Files.deleteIfExists(bak)
    }
}

inline fun <reified T : Any> dbObject(path: String) = DbObject(path, T::class.java)
