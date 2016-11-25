package com.petukhovsky.jvaluer.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

interface Script {
    fun execute()
}

abstract class LiveProcess<R>(val update: Long = 30) {

    var started: Long = -1
    var ended: Long? = null

    var result: R? = null

    abstract fun update()
    abstract fun run(): R

    fun execute(): R {
        result = null
        val thread = Thread { result = run() }
        started = now()
        ended = null
        thread.start()
        while (thread.isAlive) {
            update()
            thread.join(update)
        }
        ended = now()
        update()
        return result!!
    }

    fun now(): Long = System.currentTimeMillis()
}

fun pathJSON(string: String): Path? {
    val path1 = Paths.get(string + ".json")
    if (Files.exists(path1)) return path1
    val path2 = Paths.get(string)
    return if (Files.exists(path2)) path2 else null
}