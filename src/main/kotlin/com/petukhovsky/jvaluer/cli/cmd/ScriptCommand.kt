package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.objectMapper
import com.petukhovsky.jvaluer.cli.pathJSON
import java.nio.file.Files

object ScriptCommand : Command {
    override fun command(args: Array<String>) {
        if (args.size != 3) {
            printHelp()
            return
        }
        val path = pathJSON(args[2])
        if (path == null) {
            println("File not found")
            return
        }
        when (args[1]) {
            "run" -> Files.newInputStream(path).use {
                objectMapper.readValue<RunScript>(it).execute()
            }
            "gen" -> Files.newInputStream(path).use {
                objectMapper.readValue<GenScript>(it).execute()
            }
            else -> {
                println("Unknown command")
            }
        }
    }

    override fun printHelp() {
        println("usage: jv script <command> <file>")
        println()
        println("execute script in <file>")
        println("available commands: [run, gen]")
    }

}