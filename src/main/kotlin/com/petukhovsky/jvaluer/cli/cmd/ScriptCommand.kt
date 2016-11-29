package com.petukhovsky.jvaluer.cli.cmd

import com.fasterxml.jackson.module.kotlin.readValue
import com.petukhovsky.jvaluer.cli.copyIfNotNull
import com.petukhovsky.jvaluer.cli.objectMapper
import com.petukhovsky.jvaluer.cli.pathJSON
import java.nio.file.Files

object ScriptCommand : Command {
    override fun command(args: Array<String>) {
        if (args.size < 3) {
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
            "gen" -> {
                val script = Files.newInputStream(path).use {
                    objectMapper.readValue<GenScript>(it)
                }
                if (args.size > 3) {
                    val result = script.generate()
                    result.out.copyIfNotNull(path)
                } else script.execute()
            }
            else -> {
                println("Unknown command")
            }
        }
    }

    override fun printHelp() {
        println("usage: jv script <command> <file> [...]")
        println()
        println("execute script in <file>")
        println("available commands:")
        println("   run     run script")
        println("   gen     generate from gen script to file 'gen gen.json input.txt'")
    }

}