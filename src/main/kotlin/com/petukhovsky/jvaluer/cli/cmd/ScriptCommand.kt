package com.petukhovsky.jvaluer.cli.cmd

import com.petukhovsky.jvaluer.cli.copyIfNotNull
import com.petukhovsky.jvaluer.cli.getNullablePath
import com.petukhovsky.jvaluer.cli.pathJSON
import com.petukhovsky.jvaluer.cli.readJSON

object ScriptCommand : Command {
    override fun command(args: Array<String>) {
        if (args.size < 3) {
            printHelp()
            return
        }
        val path = pathJSON(args[2]) ?: return
        when (args[1]) {
            "run" -> readJSON<RunScript>(path).execute()
            "gen" -> {
                val script = readJSON<GenScript>(path)
                if (args.size > 3) {
                    val dest = getNullablePath(args[3])
                    val result = script.generate()
                    result.out.copyIfNotNull(dest)
                } else script.execute()
            }
            "checker" -> readJSON<CheckerScript>(path).execute()
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
        println("   checker checker script")
    }

}