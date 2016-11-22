package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.ConfigBackup
import com.petukhovsky.jvaluer.cli.backupFromConfig
import com.petukhovsky.jvaluer.cli.objectMapper
import java.nio.file.Files
import java.nio.file.Paths

object Backup : Command {
    override fun command(args: Array<String>) {
        if (args.size != 2) {
            printHelp()
            return
        }
        val filename = args[1]
        val path = Paths.get(filename)
        val backup = try {
            backupFromConfig()
        } catch (e: Exception) {
            println("Config is missing or corrupted. Run 'jv init' to fix")
            e.printStackTrace()
            return
        }
        Files.newOutputStream(path).use {
            objectMapper.writeValue(it, backup)
        }
        println("Backup is successfully created")
    }

    override fun printHelp() {
        println("Usage: jv backup <file>")
        println()
        println("Store config to the file. Then you can restore it with 'jv init --file <file>'")
    }

}