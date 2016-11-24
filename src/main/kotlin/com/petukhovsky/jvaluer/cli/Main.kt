package com.petukhovsky.jvaluer.cli

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.petukhovsky.jvaluer.cli.cmd.*
import java.io.InputStream

fun main(args: Array<String>) {
    if (args.isEmpty()) return Help.command(args)
    commandByName(args[0]).command(args)
}

fun commandByName(name: String) =
        when (name) {
            "help" -> Help
            "init" -> Init
            "backup" -> Backup
            "run" -> Run
            "run-script" -> RunScriptCmd
            else -> UnknownCommand
        }

val objectMapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT) //TODO: indent db arg
        .registerModule(JavaTimeModule())

fun readYN(default: Boolean? = true): Boolean? {
    val line = readLine() ?: return default
    if (line.isEmpty()) return default
    if (line.trim().equals("y", ignoreCase = true)) return true
    if (line.trim().equals("n", ignoreCase = true)) return false
    return null
}