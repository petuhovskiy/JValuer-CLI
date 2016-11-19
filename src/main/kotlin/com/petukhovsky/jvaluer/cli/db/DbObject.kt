package com.petukhovsky.jvaluer.cli.db

import com.petukhovsky.jvaluer.cli.objectMapper
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

private val prefix = ".jv/db/"

class DbObject<T>(path: String, val c: Class<T>) {

    val bak = Paths.get(prefix, "$path.bak")
    val json = Paths.get(prefix, "$path.json")

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
        objectMapper.writeValue(json.toFile(), value)
        Files.deleteIfExists(bak)
    }
}

inline fun <reified T : Any> dbObject(path: String) = DbObject(path, T::class.java)
