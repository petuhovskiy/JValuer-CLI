package com.petukhovsky.jvaluer.cli

import com.petukhovsky.jvaluer.cli.objectMapper
import com.petukhovsky.jvaluer.util.FilesUtils
import java.nio.file.*

val dbDir = configDir.resolve("db/")

class DbObject<T>(path: String, val c: Class<T>) {

    val bak = dbDir.resolve("$path.bak")
    val json = dbDir.resolve("$path.json")

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

    fun getOrDefault(): T {
        return get() ?: c.newInstance()
    }

    fun save(value: T) {
        Files.createDirectories(json.parent)
        if (Files.exists(json)) Files.move(json, bak, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Files.newOutputStream(json).use { objectMapper.writeValue(it, value) }
        Files.deleteIfExists(bak)
    }
}

inline fun <reified T : Any> db(path: String) = DbObject(path, T::class.java)

val objectsDir = configDir.resolve("objects/").apply { Files.createDirectories(this) }

fun saveObject(path: Path): MySHA {
    val hash = hashOf(path)
    val nPath = objectsDir.resolve(hash.string)
    if (Files.notExists(nPath) || hashOf(nPath) != hash) {
        Files.copy(path, nPath)
    }
    return hash
}

fun getObject(hash: MySHA): Path? {
    val path = objectsDir.resolve(hash.string)
    return if (Files.notExists(path)) null else path
}
